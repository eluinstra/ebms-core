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
package nl.clockwork.ebms.job;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSIrrecoverableResponsexception;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.URLManager;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.util.CPAUtils;

public class EbMSEventProcessor implements InitializingBean, Job
{
	private class HandleEventTask implements Runnable
	{
		private EbMSEvent event;
		
		public HandleEventTask(EbMSEvent event)
		{
			this.event = event;
		}
		
		@Override
		public void run()
		{
			DeliveryChannel deliveryChannel = cpaManager.getDeliveryChannel(
					event.getCpaId(),
					event.getDeliveryChannelId())
						.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getDeliveryChannelId()));
			if (event.getTimeToLive() == null || new Date().before(event.getTimeToLive()))
				sendEvent(event,deliveryChannel);
			else
				expireEvent(event);
		}

		private void sendEvent(final EbMSEvent event, final DeliveryChannel deliveryChannel)
		{
			final String url = urlManager.getURL(CPAUtils.getUri(deliveryChannel));
			try
			{
				Optional<EbMSDocument> requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
				StreamUtils.ifPresentOrElse(requestDocument,
						d -> sendMessage(event,deliveryChannel,url,d),
						() -> eventManager.deleteEvent(event.getMessageId()));
			}
			catch (final EbMSResponseException e)
			{
				logger.error("",e);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,e.getMessage());
							if ((e instanceof EbMSIrrecoverableResponsexception) || !CPAUtils.isReliableMessaging(deliveryChannel))
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
				logger.error("",e);
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
							if (!CPAUtils.isReliableMessaging(deliveryChannel))
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

		private void sendMessage(final EbMSEvent event, final DeliveryChannel deliveryChannel, final String url, EbMSDocument requestDocument)
		{
			if (event.isConfidential())
				messageEncrypter.encrypt(deliveryChannel,requestDocument);
			logger.info("Sending message " + event.getMessageId() + " to " + url);
			EbMSDocument responseDocument = ebMSClient.sendMessage(url,requestDocument);
			messageProcessor.processResponse(requestDocument,responseDocument);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						eventManager.updateEvent(event,url,EbMSEventStatus.SUCCEEDED);
						if (!CPAUtils.isReliableMessaging(deliveryChannel))
							if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED) > 0)
							{
								eventListener.onMessageDelivered(event.getMessageId());
								if (deleteEbMSAttachmentsOnMessageProcessed)
									ebMSDAO.deleteAttachments(event.getMessageId());
							}
					}
				});
		}

		private void expireEvent(final EbMSEvent event)
		{
			try
			{
				logger.warn("Expiring message " +  event.getMessageId());
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
				logger.error("",e);
			}
		}

	}

	private static final int DEFAULTWAIT = 30;
	protected transient Log logger = LogFactory.getLog(getClass());
	private ExecutorService executorService;
	private Integer maxThreads;
	private Integer processorsScaleFactor;
	private Integer queueScaleFactor;
	private EventListener eventListener;
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;
	private URLManager urlManager;
	private EventManager eventManager;
	private EbMSClient ebMSClient;
	private EbMSMessageEncrypter messageEncrypter;
	private EbMSMessageProcessor messageProcessor;
	private boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		if (processorsScaleFactor == null || processorsScaleFactor <= 0)
		{
			processorsScaleFactor = 1;
			logger.info(this.getClass().getName() + " using processors scale factor " + processorsScaleFactor);
		}
		if (maxThreads == null || maxThreads <= 0)
		{
			maxThreads = Runtime.getRuntime().availableProcessors() * processorsScaleFactor;
			logger.info(this.getClass().getName() + " using " + maxThreads + " threads");
		}
		if (queueScaleFactor == null || queueScaleFactor <= 0)
		{
			queueScaleFactor = 1;
			logger.info(this.getClass().getName() + " using queue scale factor " + queueScaleFactor);
		}
	}
	
  @Override
  public void execute()
  {
		executorService = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
		GregorianCalendar timestamp = new GregorianCalendar();
		List<EbMSEvent> events = ebMSDAO.getEventsBefore(timestamp.getTime());
		events.forEach(e -> executorService.submit(new HandleEventTask(e)));
		executorService.shutdown();
		try
		{
			while (!executorService.awaitTermination(DEFAULTWAIT,TimeUnit.MINUTES))
			{
				// just loop, waiting
			}
		}
		catch (InterruptedException e)
		{
			logger.trace(e);
		}
  }
  
	public void setMaxThreads(Integer maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setProcessorsScaleFactor(Integer processorsScaleFactor)
	{
		this.processorsScaleFactor = processorsScaleFactor;
	}

	public void setQueueScaleFactor(Integer queueScaleFactor)
	{
		this.queueScaleFactor = queueScaleFactor;
	}

	public void setEventListener(EventListener eventListener)
	{
		this.eventListener = eventListener;
	}

  public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

  public void setUrlManager(URLManager urlManager)
	{
		this.urlManager = urlManager;
	}

  public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

  public void setEventManager(EventManager eventManager)
	{
		this.eventManager = eventManager;
	}

  public void setEbMSClient(EbMSClient ebMSClient)
	{
		this.ebMSClient = ebMSClient;
	}

  public void setMessageEncrypter(EbMSMessageEncrypter messageEncrypter)
	{
		this.messageEncrypter = messageEncrypter;
	}

  public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

  public void setDeleteEbMSAttachmentsOnMessageProcessed(boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
	}
}
