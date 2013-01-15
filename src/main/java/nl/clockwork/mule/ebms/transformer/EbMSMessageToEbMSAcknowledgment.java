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
package nl.clockwork.mule.ebms.transformer;

import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.From;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.xml.xmldsig.ReferenceType;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToEbMSAcknowledgment extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private String hostname;

  public EbMSMessageToEbMSAcknowledgment()
	{
		registerSourceType(EbMSMessage.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			GregorianCalendar timestamp = new GregorianCalendar();

			EbMSMessage msg = (EbMSMessage)message.getPayload();
			MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(msg.getMessageHeader(),hostname,timestamp,EbMSMessageType.ACKNOWLEDGMENT.action());
			
			Acknowledgment acknowledgment = new Acknowledgment();

			acknowledgment.setVersion(Constants.EBMS_VERSION);
			acknowledgment.setMustUnderstand(true);

			acknowledgment.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp));
			acknowledgment.setRefToMessageId(messageHeader.getMessageData().getRefToMessageId());
			acknowledgment.setFrom(new From()); //optioneel
			acknowledgment.getFrom().getPartyId().addAll(messageHeader.getFrom().getPartyId());
			// ebMS specs 1701
			//acknowledgment.getFrom().setRole(messageHeader.getFrom().getRole());
			acknowledgment.getFrom().setRole(null);
			
			//TODO resolve actor from CPA
			acknowledgment.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
			
			if (msg.getAckRequested().isSigned() && msg.getSignature() != null)
				for (ReferenceType reference : msg.getSignature().getSignedInfo().getReference())
					acknowledgment.getReference().add(reference);

			message.setPayload(new EbMSAcknowledgment(messageHeader,acknowledgment));
			return message;
		}
		catch (DatatypeConfigurationException e)
		{
			throw new TransformerException(this,e);
		}
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
}
