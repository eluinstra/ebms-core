/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.delivery.task;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.client.EbMSClient;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.delivery.client.EbMSResponseException;
import nl.clockwork.ebms.delivery.client.EbMSUnrecoverableResponseException;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(transactionManager = "dataSourceTransactionManager")
class DeliveryTaskHandler
{
	@NonNull
	MessageEventListener messageEventListener;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	URLMapper urlMapper;
	@NonNull
	DeliveryTaskManager deliveryTaskManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	EbMSMessageEncrypter messageEncrypter;
	@NonNull
	EbMSMessageProcessor messageProcessor;
	TimedTask timedTask;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Builder
	public DeliveryTaskHandler(@NonNull MessageEventListener messageEventListener, @NonNull EbMSDAO ebMSDAO, @NonNull CPAManager cpaManager, @NonNull URLMapper urlMapper, @NonNull DeliveryTaskManager deliveryTaskManager, @NonNull EbMSHttpClientFactory ebMSClientFactory, @NonNull EbMSMessageEncrypter messageEncrypter, @NonNull EbMSMessageProcessor messageProcessor, TimedTask timedTask, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.messageEventListener = messageEventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.urlMapper = urlMapper;
		this.deliveryTaskManager = deliveryTaskManager;
		this.ebMSClientFactory = ebMSClientFactory;
		this.messageEncrypter = messageEncrypter;
		this.messageProcessor = messageProcessor;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		this.timedTask = timedTask;
	}

	public void handle(DeliveryTask task)
	{
		Runnable runnable = () ->
		{
			if (task.getTimeToLive() == null || Instant.now().isBefore(task.getTimeToLive()))
				sendTask(task);
			else
				expireTask(task);
		};
		timedTask.run(runnable);
	}

	@Async("deliveryTaskExecutor")
	public Future<Void> handleAsync(DeliveryTask task)
	{
		handle(task);
		return AsyncResult.forValue(null);
	}

	private void sendTask(final DeliveryTask task)
	{
		val receiveDeliveryChannel = cpaManager.getDeliveryChannel(
				task.getCpaId(),
				task.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",task.getCpaId(),task.getReceiveDeliveryChannelId()));
		val url = urlMapper.getURL(CPAUtils.getUri(receiveDeliveryChannel));
		val requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(task.getMessageId());
		if (requestDocument.isPresent())
			sendTask(task,receiveDeliveryChannel,url,requestDocument.get());
		else
		{
			log.info("Finished task " + task);
			deliveryTaskManager.deleteTask(task.getMessageId());
		}
	}

	private void sendTask(DeliveryTask task, DeliveryChannel receiveDeliveryChannel, String url, EbMSDocument requestDocument)
	{
		try
		{
			sendMessage(task,receiveDeliveryChannel,url,requestDocument);
		}
		catch (final EbMSResponseException e)
		{
			log.error("",e);
			handleException(task,receiveDeliveryChannel,url,e,e.getMessage());
		}
		catch (final Exception e)
		{
			log.error("",e);
			handleException(task,receiveDeliveryChannel,url,e,ExceptionUtils.getStackTrace(e));
		}
	}

	private void handleException(DeliveryTask task, DeliveryChannel receiveDeliveryChannel, String url, final Exception e, String errorMessage)
	{
		deliveryTaskManager.updateTask(task,url,DeliveryTaskStatus.FAILED,errorMessage);
		if ((e instanceof EbMSUnrecoverableResponseException) || !CPAUtils.isReliableMessaging(receiveDeliveryChannel))
			if (ebMSDAO.updateMessage(task.getMessageId(),EbMSMessageStatus.CREATED,EbMSMessageStatus.DELIVERY_FAILED) > 0)
			{
				messageEventListener.onMessageFailed(task.getMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(task.getMessageId());
			}
	}

	private void sendMessage(final DeliveryTask task, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument)
	{
		try
		{
			if (task.isConfidential())
				messageEncrypter.encrypt(receiveDeliveryChannel,requestDocument);
			log.info("Sending message " + task.getMessageId() + " to " + url);
			val responseDocument = createClient(task).sendMessage(url,requestDocument);
			handleResponse(task,receiveDeliveryChannel,url,requestDocument,responseDocument);
			log.info("Message " + task.getMessageId() + " sent");
		}
		catch (CertificateException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private void handleResponse(final DeliveryTask task, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument, final nl.clockwork.ebms.model.EbMSDocument responseDocument)
	{
		messageProcessor.processResponse(requestDocument,responseDocument);
		deliveryTaskManager.updateTask(task,url,DeliveryTaskStatus.SUCCEEDED);
		if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
			if (ebMSDAO.updateMessage(task.getMessageId(),EbMSMessageStatus.CREATED,EbMSMessageStatus.DELIVERED) > 0)
			{
				messageEventListener.onMessageDelivered(task.getMessageId());
				if (deleteEbMSAttachmentsOnMessageProcessed)
					ebMSDAO.deleteAttachments(task.getMessageId());
			}
	}

	private EbMSClient createClient(DeliveryTask task) throws CertificateException
	{
		String cpaId = task.getCpaId();
		val sendDeliveryChannel = task.getSendDeliveryChannelId() != null ?
				cpaManager.getDeliveryChannel(cpaId,task.getSendDeliveryChannelId())
				.orElse(null) : null;
		return ebMSClientFactory.getEbMSClient(cpaId,sendDeliveryChannel);
	}

	private void expireTask(final DeliveryTask task)
	{
		log.warn("Expiring message " +  task.getMessageId());
		ebMSDAO.getEbMSDocumentIfUnsent(task.getMessageId()).ifPresent(d -> updateMessage(task.getMessageId()));
		log.info("Finished task " + task);
		deliveryTaskManager.deleteTask(task.getMessageId());
	}

	private void updateMessage(final String messageId)
	{
		if (ebMSDAO.updateMessage(messageId,EbMSMessageStatus.CREATED,EbMSMessageStatus.EXPIRED) > 0)
		{
			messageEventListener.onMessageExpired(messageId);
			if (deleteEbMSAttachmentsOnMessageProcessed)
				ebMSDAO.deleteAttachments(messageId);
		}
	}
}
