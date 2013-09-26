/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.job;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import nl.clockwork.ebms.EventListener;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.signature.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class ProcessEvents implements Job
{
	private class SendEventJob implements Runnable
	{
		private EbMSEvent event;
		
		public SendEventJob(EbMSEvent event)
		{
			this.event = event;
		}
		
		@Override
		public void run()
		{
			try
			{
				EbMSMessage message = ebMSDAO.getMessage(event.getEbMSMessageId());
				CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
				if (cpa == null)
					throw new EbMSProcessingException("CPA " + message.getMessageHeader().getCPAId() + " not found!");
				EbMSDocument requestDocument = new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
				signatureGenerator.generate(cpa,requestDocument,message.getMessageHeader());
				String uri = CPAUtils.getUri(cpa,message);
				logger.info("Sending message. MessageId: " +  message.getMessageHeader().getMessageData().getMessageId());
				EbMSDocument responseDocument = ebMSClient.sendMessage(uri,requestDocument);
				messageProcessor.processResponse(requestDocument,responseDocument);
				updateEvent(event,EbMSEventStatus.PROCESSED,null);
			}
			catch (EbMSResponseException e)
			{
				updateEvent(event,EbMSEventStatus.FAILED,e.getMessage());
				logger.error("",e);
			}
			catch (Exception e)
			{
				updateEvent(event,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
				logger.error("",e);
			}
		}
	}

	private class ExpireEventJob implements Runnable
	{
		private EbMSEvent event;
		
		public ExpireEventJob(EbMSEvent event)
		{
			this.event = event;
		}

		@Override
		public void run()
		{
			try
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							EbMSMessage message = ebMSDAO.getMessage(event.getEbMSMessageId());
							logger.info("Expiring message. MessageId " +  message.getMessageHeader().getMessageData().getMessageId());
							updateEvent(event,EbMSEventStatus.PROCESSED,null);
							ebMSDAO.deleteEvents(event.getEbMSMessageId(),EbMSEventStatus.UNPROCESSED);
							ebMSDAO.updateMessageStatus(event.getEbMSMessageId(),null,EbMSMessageStatus.NOT_ACKNOWLEDGED);
							eventListener.onMessageNotAcknowledged(message.getMessageHeader().getMessageData().getMessageId());
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
	private int maxThreads = 4;
	private EventListener eventListener;
  private EbMSDAO ebMSDAO;
	private EbMSSignatureGenerator signatureGenerator;
  private EbMSClient ebMSClient;
	private EbMSMessageProcessor messageProcessor;

	public void init()
	{
		executorService = Executors.newFixedThreadPool(maxThreads);
	}
	
  @Override
  public void execute()
  {
  	GregorianCalendar timestamp = new GregorianCalendar();
  	List<EbMSEvent> events = ebMSDAO.getLatestEventsByEbMSMessageIdBefore(timestamp.getTime(),EbMSEventStatus.UNPROCESSED);
  	List<Future<?>> futures = new ArrayList<Future<?>>();
  	for (final EbMSEvent event : events)
			if (EbMSEventType.SEND.equals(event.getType()))
	  		futures.add(executorService.submit(new SendEventJob(event)));
			else if (EbMSEventType.EXPIRE.equals(event.getType()))
	  		futures.add(executorService.submit(new ExpireEventJob(event)));
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
  
	private void updateEvent(final EbMSEvent event, final EbMSEventStatus status, final String errorMessage)
	{
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					ebMSDAO.updateEvent(event.getTime(),event.getEbMSMessageId(),status,errorMessage);
					ebMSDAO.deleteEventsBefore(event.getTime(),event.getEbMSMessageId(),EbMSEventStatus.UNPROCESSED);
				}
			}
		);
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setEventListener(EventListener eventListener)
	{
		this.eventListener = eventListener;
	}

  public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
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
