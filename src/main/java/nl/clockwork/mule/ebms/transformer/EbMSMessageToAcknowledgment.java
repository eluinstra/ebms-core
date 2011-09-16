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

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.Acknowledgment;
import nl.clockwork.mule.ebms.model.EbMSMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToAcknowledgment extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;

  public EbMSMessageToAcknowledgment()
	{
		registerSourceType(EbMSMessage.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessage msg = (EbMSMessage)message.getPayload();
			Acknowledgment acknowledgment = ebMSDAO.getAcknowledgment(msg.getMessageHeader().getMessageData().getMessageId());
			message.setProperty("EBMS.ACKNOWLEDGMENT_TYPE",acknowledgment.getAcknowledgmentType());
			message.setPayload(new Object[]{acknowledgment.getMessageHeader(),acknowledgment.getAcknowledgment()});
			return message;
		}
		catch (DAOException e)
		{
			throw new TransformerException(this,e);
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
