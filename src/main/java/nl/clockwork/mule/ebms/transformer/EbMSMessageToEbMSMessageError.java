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
import nl.clockwork.mule.ebms.model.ebxml.Error;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.ebxml.SeverityType;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToEbMSMessageError extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private String hostname;

  public EbMSMessageToEbMSMessageError()
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

			List<PartyId> partyIds = new ArrayList<PartyId>(messageHeader.getFrom().getPartyId());
			messageHeader.getFrom().getPartyId().clear();
			messageHeader.getFrom().getPartyId().addAll(messageHeader.getTo().getPartyId());
			messageHeader.getTo().getPartyId().clear();
			messageHeader.getTo().getPartyId().addAll(partyIds);
			
			messageHeader.getFrom().setRole(null);
			messageHeader.getTo().setRole(null);

			messageHeader.getMessageData().setRefToMessageId(messageHeader.getMessageData().getMessageId());
			messageHeader.getMessageData().setMessageId(message.getCorrelationId() + "-" + new Date().getTime() + "@" + hostname);
			messageHeader.getMessageData().setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar));

			messageHeader.getService().setType(null);
			messageHeader.getService().setValue(Constants.EBMS_SERVICE);
			messageHeader.setAction(Constants.EBMS_MESSAGE_ERROR);

			messageHeader.setDuplicateElimination(null);

			ErrorList errorList = new ErrorList();

			errorList.setVersion(Constants.EBMS_VERSION);
			errorList.setMustUnderstand(true);
			errorList.setHighestSeverity(SeverityType.ERROR);

			Error error = (Error)message.getProperty(Constants.EBMS_ERROR);
			if (error == null)
				EbMSMessageUtils.createError(Constants.EbMSErrorLocation.UNKNOWN.location(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!");
			errorList.getError().add(error);
			
			message.setPayload(new Object[]{messageHeader,errorList});
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
