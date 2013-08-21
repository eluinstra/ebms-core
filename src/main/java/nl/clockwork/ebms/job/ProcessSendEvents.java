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

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class ProcessSendEvents implements Job
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private ExecutorService executorService;
	private int maxThreads = 4;
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
  	List<EbMSSendEvent> sendEvents = ebMSDAO.getLatestEventsByEbMSMessageIdBefore(timestamp.getTime(),EbMSEventStatus.UNPROCESSED);
  	List<Future<?>> futures = new ArrayList<Future<?>>();
  	for (final EbMSSendEvent sendEvent : sendEvents)
  	{
  		futures.add(
  			executorService.submit(
	  			new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								EbMSMessage message = ebMSDAO.getMessage(sendEvent.getEbMSMessageId());
								CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
								EbMSDocument requestDocument = new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
								signatureGenerator.generate(cpa,requestDocument,message.getMessageHeader());
								String uri = CPAUtils.getUri(cpa,message);
								logger.info("Sending message. MessageId: " +  message.getMessageHeader().getMessageData().getMessageId());
								EbMSDocument responseDocument = ebMSClient.sendMessage(uri,requestDocument);
								messageProcessor.processResponse(requestDocument,responseDocument);
								updateEvent(sendEvent,EbMSEventStatus.PROCESSED,null);
							}
							catch (EbMSResponseException e)
							{
								updateEvent(sendEvent,EbMSEventStatus.FAILED,e.getMessage());
								logger.error("",e);
							}
							catch (Exception e)
							{
								updateEvent(sendEvent,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
								logger.error("",e);
							}
						}
					}
  			)
  		);
  	}
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
  
	private void updateEvent(final EbMSSendEvent sendEvent, final EbMSEventStatus status, final String errorMessage)
	{
		ebMSDAO.executeTransaction(
  			new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.updateSendEvent(sendEvent.getTime(),sendEvent.getEbMSMessageId(),status,errorMessage);
						ebMSDAO.deleteEventsBefore(sendEvent.getTime(),sendEvent.getEbMSMessageId(),EbMSEventStatus.UNPROCESSED);
					}
				}
  		);
	}

	public void setMaxThreads(int maxThreads)
	{
		this.maxThreads = maxThreads;
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
