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

import java.util.GregorianCalendar;
import java.util.List;

import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.Transport;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessSendEvents implements Job
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;
	private EbMSSignatureGenerator signatureGenerator;
  private EbMSClient ebMSClient;

  @Override
  public void execute()
  {
  	try
  	{
	  	GregorianCalendar timestamp = new GregorianCalendar();
	  	List<EbMSSendEvent> sendEvents = ebMSDAO.selectEventsForSending(timestamp.getTime());
	  	for (final EbMSSendEvent sendEvent : sendEvents)
	  	{
	  		EbMSMessage message = ebMSDAO.getMessage(sendEvent.getEbMSMessageId());
	  		EbMSDocument document = new EbMSDocument(EbMSMessageUtils.createSOAPMessage(message),message.getAttachments());
	  		signatureGenerator.generateSignature(document.getMessage(),document.getAttachments());
	  		String uri = getUrl(message);
	  		ebMSClient.sendMessage(uri,document);
	  		ebMSDAO.executeTransaction(
	  			new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
				  		ebMSDAO.updateSentEvent(sendEvent.getTime(),sendEvent.getEbMSMessageId());
				  		ebMSDAO.deleteUnprocessedEvents(sendEvent.getTime(),sendEvent.getEbMSMessageId());
						}
					}
	  		);
	  	}
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
