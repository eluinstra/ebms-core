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
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSResponseDocument;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.Transport;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessSendEvents implements Job
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private ExecutorService executorService;
	private int maxThreads = 4;
  private EbMSDAO ebMSDAO;
	private EbMSSignatureGenerator signatureGenerator;
  private EbMSClient ebMSClient;
	private EbMSMessageProcessor ebMSMessageProcessor;

	public void init()
	{
		executorService = Executors.newFixedThreadPool(maxThreads);
	}
	
  @Override
  public void run()
  {
  	GregorianCalendar timestamp = new GregorianCalendar();
  	List<EbMSSendEvent> sendEvents = ebMSDAO.selectEventsForSending(timestamp.getTime());
  	List<Future<?>> futures = new ArrayList<Future<?>>();
  	for (final EbMSSendEvent sendEvent : sendEvents)
  	{
  		Future<?> future = executorService.submit(
  			new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
				  		EbMSMessage message = ebMSDAO.getMessage(sendEvent.getEbMSMessageId());
				  		CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
				  		EbMSDocument document = new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
				  		signatureGenerator.generate(cpa,document,message.getMessageHeader());
				  		String uri = getUrl(message);
				  		EbMSDocument responseDocument = ebMSClient.sendMessage(uri,document);
				  		if (!(responseDocument == null || (responseDocument instanceof EbMSResponseDocument && ((EbMSResponseDocument)responseDocument).getMessage() == null)))
				  			ebMSMessageProcessor.process(responseDocument);
				  		updateEvent(sendEvent,EbMSEventStatus.PROCESSED);
						}
						catch (Exception e)
						{
			  			updateEvent(sendEvent,EbMSEventStatus.FAILED);
				  		logger.error("",e);
						}
					}
				}
  		);
  		futures.add(future);
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
  
	private String getUrl(EbMSMessage message)
	{
		CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(message.getMessageHeader().getCPAId());
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,message.getMessageHeader().getTo().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getReceivingDeliveryChannels(partyInfo,message.getMessageHeader().getTo().getRole(),message.getMessageHeader().getService(),message.getMessageHeader().getAction()).get(0);
		Transport transport = (Transport)deliveryChannel.getTransportId();
		return transport.getTransportReceiver().getEndpoint().get(0).getUri();
	}

	private void updateEvent(final EbMSSendEvent sendEvent, final EbMSEventStatus status)
	{
		ebMSDAO.executeTransaction(
  			new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
			  		ebMSDAO.updateSendEvent(sendEvent.getTime(),sendEvent.getEbMSMessageId(),status);
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
}
