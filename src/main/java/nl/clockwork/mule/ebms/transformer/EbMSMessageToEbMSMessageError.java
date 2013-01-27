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

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.ebxml.Error;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.util.EbMSMessageUtils;

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
			GregorianCalendar timestamp = new GregorianCalendar();

			EbMSMessage msg = (EbMSMessage)message.getPayload();
			MessageHeader messageHeader = EbMSMessageUtils.createMessageHeader(msg.getMessageHeader(),hostname,timestamp,EbMSMessageType.MESSAGE_ERROR.action());
			
			ErrorList errorList = new ErrorList();

			errorList.setVersion(Constants.EBMS_VERSION);
			errorList.setMustUnderstand(true);
			errorList.setHighestSeverity(SeverityType.ERROR);

			Error error = (Error)message.getProperty(Constants.EBMS_ERROR);
			if (error == null)
				error = EbMSMessageUtils.createError(Constants.EbMSErrorLocation.UNKNOWN.location(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"An unknown error occurred!");
			errorList.getError().add(error);
			
			message.setPayload(new EbMSMessageError(messageHeader,errorList));
			return message;
		}
		catch (DatatypeConfigurationException e)
		{
			throw new TransformerException(this,e);
		}
		catch (JAXBException e)
		{
			throw new TransformerException(this,e);
		}
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
}
