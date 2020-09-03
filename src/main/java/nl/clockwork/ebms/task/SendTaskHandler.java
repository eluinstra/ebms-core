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
package nl.clockwork.ebms.task;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.Future;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.client.EbMSUnrecoverableResponseException;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class SendTaskHandler
{
	@NonNull
	PlatformTransactionManager transactionManager;
	@NonNull
	MessageEventListener messageEventListener;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	URLMapper urlMapper;
	@NonNull
	SendTaskManager sendTaskManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	EbMSMessageEncrypter messageEncrypter;
	@NonNull
	EbMSMessageProcessor messageProcessor;
	TimedTask timedTask;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Builder
	public SendTaskHandler(@NonNull PlatformTransactionManager transactionManager, @NonNull MessageEventListener messageEventListener, @NonNull EbMSDAO ebMSDAO, @NonNull CPAManager cpaManager, @NonNull URLMapper urlMapper, @NonNull SendTaskManager sendTaskManager, @NonNull EbMSHttpClientFactory ebMSClientFactory, @NonNull EbMSMessageEncrypter messageEncrypter, @NonNull EbMSMessageProcessor messageProcessor, TimedTask timedTask, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.transactionManager = transactionManager;
		this.messageEventListener = messageEventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.urlMapper = urlMapper;
		this.sendTaskManager = sendTaskManager;
		this.ebMSClientFactory = ebMSClientFactory;
		this.messageEncrypter = messageEncrypter;
		this.messageProcessor = messageProcessor;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		this.timedTask = timedTask;
	}

	public void handle(SendTask task)
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

	@Async("sendTaskExecutor")
	public Future<Void> handleAsync(SendTask task)
	{
		Runnable runnable = () ->
		{
			if (task.getTimeToLive() == null || Instant.now().isBefore(task.getTimeToLive()))
				sendTask(task);
			else
				expireTask(task);
		};
		timedTask.run(runnable);
		return AsyncResult.forValue(null);
	}

	private void sendTask(final SendTask task)
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			val receiveDeliveryChannel = cpaManager.getDeliveryChannel(
					task.getCpaId(),
					task.getReceiveDeliveryChannelId())
						.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",task.getCpaId(),task.getReceiveDeliveryChannelId()));
			val url = urlMapper.getURL(CPAUtils.getUri(receiveDeliveryChannel));
			val requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(task.getMessageId());
			if (!requestDocument.isPresent())
				sendTaskManager.deleteTask(task.getMessageId());
			transactionManager.commit(status);
			requestDocument.ifPresent(d -> sendTask(task,receiveDeliveryChannel,url,d));
		}
		catch(Exception e)
		{
			if (!status.isCompleted())
				transactionManager.commit(status);
			throw e;
		}
	}

	private void sendTask(SendTask task, DeliveryChannel receiveDeliveryChannel, String url, EbMSDocument requestDocument)
	{
		try
		{
			sendMessage(task,receiveDeliveryChannel,url,requestDocument);
		}
		catch (final EbMSResponseException e)
		{
			val status = transactionManager.getTransaction(null);
			try
			{
				log.error("",e);
				sendTaskManager.updateTask(task,url,SendTaskStatus.FAILED,e.getMessage());
				if ((e instanceof EbMSUnrecoverableResponseException) || !CPAUtils.isReliableMessaging(receiveDeliveryChannel))
					if (ebMSDAO.updateMessage(task.getMessageId(),EbMSMessageStatus.CREATED,EbMSMessageStatus.DELIVERY_FAILED) > 0)
					{
						messageEventListener.onMessageFailed(task.getMessageId());
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(task.getMessageId());
					}
			}
			catch (Exception e1)
			{
				transactionManager.rollback(status);
				throw e1;
			}
			transactionManager.commit(status);
		}
		catch (final Exception e)
		{
			val status = transactionManager.getTransaction(null);
			try
			{
				log.error("",e);
				sendTaskManager.updateTask(task,url,SendTaskStatus.FAILED,ExceptionUtils.getStackTrace(e));
				if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
					if (ebMSDAO.updateMessage(task.getMessageId(),EbMSMessageStatus.CREATED,EbMSMessageStatus.DELIVERY_FAILED) > 0)
					{
						messageEventListener.onMessageFailed(task.getMessageId());
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(task.getMessageId());
					}
			}
			catch (Exception e1)
			{
				transactionManager.rollback(status);
				throw e1;
			}
			transactionManager.commit(status);
		}
	}

	private void sendMessage(final SendTask task, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument)
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

	private void handleResponse(final SendTask task, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument, final nl.clockwork.ebms.model.EbMSDocument responseDocument)
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			messageProcessor.processResponse(requestDocument,responseDocument);
			sendTaskManager.updateTask(task,url,SendTaskStatus.SUCCEEDED);
			if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
				if (ebMSDAO.updateMessage(task.getMessageId(),EbMSMessageStatus.CREATED,EbMSMessageStatus.DELIVERED) > 0)
				{
					messageEventListener.onMessageDelivered(task.getMessageId());
					if (deleteEbMSAttachmentsOnMessageProcessed)
						ebMSDAO.deleteAttachments(task.getMessageId());
				}
		}
		catch (Exception e)
		{
			transactionManager.rollback(status);
			throw e;
		}
		transactionManager.commit(status);
	}

	private EbMSClient createClient(SendTask task) throws CertificateException
	{
		String cpaId = task.getCpaId();
		val sendDeliveryChannel = task.getSendDeliveryChannelId() != null ?
				cpaManager.getDeliveryChannel(cpaId,task.getSendDeliveryChannelId())
				.orElse(null) : null;
		return ebMSClientFactory.getEbMSClient(cpaId,sendDeliveryChannel);
	}

	private void expireTask(final SendTask task)
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			log.warn("Expiring message " +  task.getMessageId());
			ebMSDAO.getEbMSDocumentIfUnsent(task.getMessageId()).ifPresent(d -> updateMessage(task.getMessageId()));
			sendTaskManager.deleteTask(task.getMessageId());
		}
		catch (Exception e)
		{
			transactionManager.rollback(status);
			throw e;
		}
		transactionManager.commit(status);
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
