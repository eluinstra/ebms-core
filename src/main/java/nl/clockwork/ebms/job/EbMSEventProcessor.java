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

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.client.EbMSResponseSOAPException;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.util.CPAUtils;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

public class EbMSEventProcessor implements Job
{
	private class HandleEventJob implements Runnable
	{
		private EbMSEvent event;
		
		public HandleEventJob(EbMSEvent event)
		{
			this.event = event;
		}
		
		@Override
		public void run()
		{
			DeliveryChannel deliveryChannel = cpaManager.getDeliveryChannel(event.getCpaId(),event.getDeliveryChannelId());
			if (event.getTimeToLive().after(new Date()))
				sendEvent(event,deliveryChannel);
			else
				expireEvent(event);
		}

		private void sendEvent(final EbMSEvent event, DeliveryChannel deliveryChannel)
		{
			String url = null;
			try
			{
				EbMSDocument requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
				if (requestDocument != null)
				{
					url = CPAUtils.getUri(deliveryChannel);
					logger.info("Sending message " + event.getMessageId() + " to " + url);
					EbMSDocument responseDocument = ebMSClient.sendMessage(url,requestDocument);
					messageProcessor.processResponse(requestDocument,responseDocument);
					eventManager.updateEvent(event,url,EbMSEventStatus.SUCCEEDED);
				}
				else
					eventManager.deleteEvent(event.getMessageId());
			}
			catch (final EbMSResponseException e)
			{
				final String url_ = url;
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							eventManager.updateEvent(event,url_,EbMSEventStatus.FAILED,e.getMessage());
							if (e instanceof EbMSResponseSOAPException && EbMSResponseSOAPException.CLIENT.equals(((EbMSResponseSOAPException)e).getFaultCode()))
							{
								ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.DELIVERY_FAILED);
								eventListener.onMessageFailed(event.getMessageId());
							}
						}
					}
				);
				logger.error("",e);
			}
			catch (Exception e)
			{
				eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
				logger.error("",e);
			}
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
							EbMSDocument requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
							if (requestDocument != null)
							{
								eventManager.updateEvent(event,null,EbMSEventStatus.SUCCEEDED);
								ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENT,EbMSMessageStatus.EXPIRED);
								eventListener.onMessageExpired(event.getMessageId());
							}
							else
								eventManager.deleteEvent(event.getMessageId());
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

	protected transient Log logger = LogFactory.getLog(getClass());
	private ExecutorService executorService;
	private Integer maxThreads;
	private Integer processorsScaleFactor;
	private Integer queueScaleFactor;
	private EventListener eventListener;
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;
	private EventManager eventManager;
	private EbMSClient ebMSClient;
	private EbMSMessageProcessor messageProcessor;

	public void init()
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
		//executorService = Executors.newFixedThreadPool(maxThreads);
		executorService = new ThreadPoolExecutor(maxThreads,maxThreads,1,TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(maxThreads * queueScaleFactor,true),new ThreadPoolExecutor.CallerRunsPolicy());
  	GregorianCalendar timestamp = new GregorianCalendar();
  	List<EbMSEvent> events = ebMSDAO.getEventsBefore(timestamp.getTime());
  	for (final EbMSEvent event : events)
  		executorService.submit(new HandleEventJob(event));
  	executorService.shutdown();
  	try
		{
			while (!executorService.awaitTermination(1,TimeUnit.HOURS));
		}
		catch (InterruptedException e)
		{
			logger.trace(e);
		}
  }
  
  public void executeOld()
  {
  	GregorianCalendar timestamp = new GregorianCalendar();
  	List<EbMSEvent> events = ebMSDAO.getEventsBefore(timestamp.getTime());
  	List<Future<?>> futures = new ArrayList<Future<?>>();
  	for (final EbMSEvent event : events)
  		futures.add(executorService.submit(new HandleEventJob(event)));
  	for (Future<?> future : futures)
			try
			{
				future.get();
			}
			catch (Exception e)
			{
	  		logger.error("",e);
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

  public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
}
