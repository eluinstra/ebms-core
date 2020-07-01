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
package nl.clockwork.ebms.event.processor;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.scheduling.annotation.Async;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.Action;
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
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.transaction.TransactionTemplate;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventHandler
{
	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	EventListener eventListener;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	URLMapper urlMapper;
	@NonNull
	EventManager eventManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	EbMSMessageEncrypter messageEncrypter;
	@NonNull
	EbMSMessageProcessor messageProcessor;
	TimedAction timedAction;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Builder
	public EventHandler(@NonNull TransactionTemplate transactionTemplate, @NonNull EventListener eventListener, @NonNull EbMSDAO ebMSDAO, @NonNull CPAManager cpaManager, @NonNull URLMapper urlMapper, @NonNull EventManager eventManager, @NonNull EbMSHttpClientFactory ebMSClientFactory, @NonNull EbMSMessageEncrypter messageEncrypter, @NonNull EbMSMessageProcessor messageProcessor, TimedAction timedAction, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.transactionTemplate = transactionTemplate;
		this.eventListener = eventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.urlMapper = urlMapper;
		this.eventManager = eventManager;
		this.ebMSClientFactory = ebMSClientFactory;
		this.messageEncrypter = messageEncrypter;
		this.messageProcessor = messageProcessor;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		this.timedAction = timedAction;
	}

	public void handle(EbMSEvent event)
	{
		Action action = () ->
		{
			if (event.getTimeToLive() == null || Instant.now().isBefore(event.getTimeToLive()))
				sendEvent(event);
			else
				expireEvent(event);
		};
		timedAction.run(action);
	}

	@Async("eventHandlerTaskExecutor")
	public CompletableFuture<Object> handleAsync(EbMSEvent event)
	{
		Action action = () ->
		{
			if (event.getTimeToLive() == null || Instant.now().isBefore(event.getTimeToLive()))
				sendEvent(event);
			else
				expireEvent(event);
		};
		timedAction.run(action);
		return CompletableFuture.completedFuture(new Object());
	}

	private void sendEvent(final EbMSEvent event)
	{
		val receiveDeliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		val url = urlMapper.getURL(CPAUtils.getUri(receiveDeliveryChannel));
		val requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
		if (!requestDocument.isPresent())
			eventManager.deleteEvent(event.getMessageId());
		requestDocument.ifPresent(d -> sendEvent(event,receiveDeliveryChannel,url,d));
	}

	private void sendEvent(EbMSEvent event, DeliveryChannel receiveDeliveryChannel, String url, EbMSDocument requestDocument)
	{
		try
		{
			sendMessage(event,receiveDeliveryChannel,url,requestDocument);
		}
		catch (final EbMSResponseException e)
		{
			Action action = () ->
			{
				log.error("",e);
				eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,e.getMessage());
				if ((e instanceof EbMSUnrecoverableResponseException) || !CPAUtils.isReliableMessaging(receiveDeliveryChannel))
					if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
					{
						eventListener.onMessageFailed(event.getMessageId());
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(event.getMessageId());
					}
			};
			transactionTemplate.executeTransaction(action);
		}
		catch (final Exception e)
		{
			Action action = () ->
			{
				log.error("",e);
				eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
				if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
					if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
					{
						eventListener.onMessageFailed(event.getMessageId());
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(event.getMessageId());
					}
			};
			transactionTemplate.executeTransaction(action);
		}
	}

	private void sendMessage(final EbMSEvent event, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument)
	{
		try
		{
			if (event.isConfidential())
				messageEncrypter.encrypt(receiveDeliveryChannel,requestDocument);
			log.info("Sending message " + event.getMessageId() + " to " + url);
			val responseDocument = createClient(event).sendMessage(url,requestDocument);
			handleResponse(event,receiveDeliveryChannel,url,requestDocument,responseDocument);
		}
		catch (CertificateException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private void handleResponse(final EbMSEvent event, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument, final nl.clockwork.ebms.model.EbMSDocument responseDocument)
	{
		Action action = () ->
		{
			messageProcessor.processResponse(requestDocument,responseDocument);
			eventManager.updateEvent(event,url,EbMSEventStatus.SUCCEEDED);
			if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
				if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED) > 0)
				{
					eventListener.onMessageDelivered(event.getMessageId());
					if (deleteEbMSAttachmentsOnMessageProcessed)
						ebMSDAO.deleteAttachments(event.getMessageId());
				}
		};
		transactionTemplate.executeTransaction(action);
	}

	private EbMSClient createClient(EbMSEvent event) throws CertificateException
	{
		String cpaId = event.getCpaId();
		val sendDeliveryChannel = event.getSendDeliveryChannelId() != null ?
				cpaManager.getDeliveryChannel(cpaId,event.getSendDeliveryChannelId())
				.orElse(null) : null;
		return ebMSClientFactory.getEbMSClient(cpaId,sendDeliveryChannel);
	}

	private void expireEvent(final EbMSEvent event)
	{
		Action action = () ->
		{
			log.warn("Expiring message " +  event.getMessageId());
			ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId()).ifPresent(d -> updateMessage(event.getMessageId()));
			eventManager.deleteEvent(event.getMessageId());
		};
		transactionTemplate.executeTransaction(action);
	}

	private void updateMessage(final String messageId)
	{
		if (ebMSDAO.updateMessage(messageId,EbMSMessageStatus.SENDING,EbMSMessageStatus.EXPIRED) > 0)
		{
			eventListener.onMessageExpired(messageId);
			if (deleteEbMSAttachmentsOnMessageProcessed)
				ebMSDAO.deleteAttachments(messageId);
		}
	}
}
