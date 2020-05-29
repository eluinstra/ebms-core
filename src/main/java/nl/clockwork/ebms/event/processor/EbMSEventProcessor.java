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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
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
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSEventProcessor implements Runnable
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private class HandleEventTask implements Runnable
	{
		@NonNull
		EbMSEvent event;
		
		@Override
		public void run()
		{
			if (event.getTimeToLive() == null || Instant.now().isBefore(event.getTimeToLive()))
				sendEvent(event);
			else
				expireEvent(event);
		}

		private void sendEvent(final EbMSEvent event)
		{
			val receiveDeliveryChannel = cpaManager.getDeliveryChannel(
					event.getCpaId(),
					event.getReceiveDeliveryChannelId())
						.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
			val url = urlMapper.getURL(CPAUtils.getUri(receiveDeliveryChannel));
			try
			{
				val requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
				StreamUtils.ifPresentOrElse(requestDocument,
						d -> sendMessage(event,receiveDeliveryChannel,url,d),
						() -> eventManager.deleteEvent(event.getMessageId()));
			}
			catch (final EbMSResponseException e)
			{
				log.error("",e);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,e.getMessage());
							if ((e instanceof EbMSUnrecoverableResponseException) || !CPAUtils.isReliableMessaging(receiveDeliveryChannel))
								if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
								{
									eventListener.onMessageFailed(event.getMessageId());
									if (deleteEbMSAttachmentsOnMessageProcessed)
										ebMSDAO.deleteAttachments(event.getMessageId());
								}
						}
					}
				);
			}
			catch (final Exception e)
			{
				log.error("",e);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
							if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
								if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
								{
									eventListener.onMessageFailed(event.getMessageId());
									if (deleteEbMSAttachmentsOnMessageProcessed)
										ebMSDAO.deleteAttachments(event.getMessageId());
								}
						}
					}
				);
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
				messageProcessor.processResponse(requestDocument,responseDocument);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url,EbMSEventStatus.SUCCEEDED);
							if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
								if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED) > 0)
								{
									eventListener.onMessageDelivered(event.getMessageId());
									if (deleteEbMSAttachmentsOnMessageProcessed)
										ebMSDAO.deleteAttachments(event.getMessageId());
								}
						}
					});
			}
			catch (CertificateException e)
			{
				throw new EbMSProcessingException(e);
			}
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
			try
			{
				log.warn("Expiring message " +  event.getMessageId());
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId()).ifPresent(d -> updateMessage(event.getMessageId()));
							eventManager.deleteEvent(event.getMessageId());
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
				);
			}
			catch (Exception e)
			{
				log.error("",e);
			}
		}

	}

	int delay;
	int period;
	int maxEvents;
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
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	ExecutorService executorService;

	@Builder(setterPrefix = "set")
	public EbMSEventProcessor(
			boolean enabled,
			int delay,
			int period,
			Integer maxThreads,
			Integer processorsScaleFactor,
			Integer queueScaleFactor,
			int maxEvents,
			@NonNull EventListener eventListener,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull CPAManager cpaManager,
			@NonNull URLMapper urlMapper,
			@NonNull EventManager eventManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory,
			@NonNull EbMSMessageEncrypter messageEncrypter,
			@NonNull EbMSMessageProcessor messageProcessor,
			boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.delay = delay;
		this.period = period;
		this.maxEvents = maxEvents;
		this.eventListener = eventListener;
		this.ebMSDAO = ebMSDAO;
		this.cpaManager = cpaManager;
		this.urlMapper = urlMapper;
		this.eventManager = eventManager;
		this.ebMSClientFactory = ebMSClientFactory;
		this.messageEncrypter = messageEncrypter;
		this.messageProcessor = messageProcessor;
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
		if (enabled)
		{
			if (processorsScaleFactor == null || processorsScaleFactor <= 0)
			{
				processorsScaleFactor = 1;
				log.info(this.getClass().getName() + " using processors scale factor " + processorsScaleFactor);
			}
			if (maxThreads == null || maxThreads <= 0)
			{
				maxThreads = Runtime.getRuntime().availableProcessors() * processorsScaleFactor;
				log.info(this.getClass().getName() + " using " + maxThreads + " threads");
			}
			if (queueScaleFactor == null || queueScaleFactor <= 0)
			{
				queueScaleFactor = 1;
				log.info(this.getClass().getName() + " using queue scale factor " + queueScaleFactor);
			}
			this.executorService = new ThreadPoolExecutor(
					maxThreads,
					maxThreads,
					1,
					TimeUnit.MINUTES,
					new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),
					new ThreadPoolExecutor.CallerRunsPolicy());
			val thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
		else
			this.executorService = null;
	}

  public void run()
  {
		sleep(delay);
  	while (true)
  	{
  		val start = Instant.now();
			val futures = new ArrayList<Future<?>>();
			try
			{
				val timestamp = Instant.now();
				val events = maxEvents > 0 ? eventManager.getEventsBefore(timestamp,maxEvents) : eventManager.getEventsBefore(timestamp);
				for (EbMSEvent event : events)
					futures.add(executorService.submit(new HandleEventTask(event)));
			}
			catch (Exception e)
			{
				log.error("",e);
			}
			futures.forEach(f -> Try.of(() -> f.get()).onFailure(e -> log.error("",e)));
			val end = Instant.now();
			val sleep = period - ChronoUnit.MILLIS.between(start,end);
			sleep(sleep);
  	}
  }

	private void sleep(long millis)
	{
		try
		{
			if (millis > 0)
				Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			log.trace("",e);
		}
	}
}
