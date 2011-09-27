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

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.Signature;
import nl.clockwork.mule.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.mule.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.mule.ebms.model.ebxml.From;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.xml.xmldsig.ReferenceType;

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
			GregorianCalendar calendar = new GregorianCalendar();

			EbMSMessage msg = (EbMSMessage)message.getPayload();
			MessageHeader messageHeader = msg.getMessageHeader();

			Acknowledgment acknowledgment = new Acknowledgment();

			acknowledgment.setVersion(Constants.EBMS_VERSION);
			acknowledgment.setMustUnderstand(true);

			acknowledgment.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));
			acknowledgment.setRefToMessageId(messageHeader.getMessageData().getMessageId());
			acknowledgment.setFrom(new From()); //optioneel
			acknowledgment.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
			// ebMS specs 1701
			//acknowledgment.getFrom().setRole(messageHeader.getTo().getRole());
			acknowledgment.getFrom().setRole(null);
			
			acknowledgment.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
			
			Signature signature = msg.getSignature();
			if (signature != null)
				for (ReferenceType reference : signature.getSignature().getSignedInfo().getReference())
					acknowledgment.getReference().add(reference);

			List<PartyId> partyIds = new ArrayList<PartyId>(messageHeader.getFrom().getPartyId());

			messageHeader.getFrom().getPartyId().clear();
			messageHeader.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
			// ebMS specs 1701
			//messageHeader.getFrom().setRole(messageHeader.getTo().getRole());
			messageHeader.getFrom().setRole(null);

			messageHeader.getTo().getPartyId().clear();
			messageHeader.getTo().getPartyId().addAll(partyIds);
			// ebMS specs 1703
			//messageHeader.getTo().setRole(role);
			messageHeader.getTo().setRole(null);

			messageHeader.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
			messageHeader.getMessageData().setMessageId(message.getCorrelationId() + "-" + new Date().getTime() + "@" + hostname);
			messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));

			messageHeader.getService().setType(null);
			messageHeader.getService().setValue(Constants.EBMS_SERVICE);
			messageHeader.setAction(Constants.EBMS_ACKNOWLEDGEMENT);

			messageHeader.setDuplicateElimination(null);

			//FIXME??? remove null values
			message.setPayload(new Object[]{messageHeader,null,null,null,acknowledgment,null,null,null,null});
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
