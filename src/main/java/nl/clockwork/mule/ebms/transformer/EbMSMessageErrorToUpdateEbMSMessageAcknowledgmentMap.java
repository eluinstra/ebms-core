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

import java.util.HashMap;

import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageErrorToUpdateEbMSMessageAcknowledgmentMap extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());

  public EbMSMessageErrorToUpdateEbMSMessageAcknowledgmentMap()
	{
		//registerSourceType(EbMSMessageError.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			//EbMSMessageError messageError = (EbMSMessageError)message.getPayload();
			Object[] messageError = (Object[])message.getPayload();
			HashMap<String,Object> map = new HashMap<String,Object>();
			map.put("id",message.getStringProperty(Constants.EBMS_MESSAGE_ID,null));
			map.put("ack_type",Constants.EbMSAcknowledgmentType.MESSAGE_ERROR.id());
			map.put("ack_header",XMLMessageBuilder.getInstance(MessageHeader.class).handle((MessageHeader)messageError[0]));
			map.put("ack_content",XMLMessageBuilder.getInstance(ErrorList.class).handle((ErrorList)messageError[5]));
			message.setPayload(map);
		}
		catch (Exception e)
		{
			logger.error("",e);
			throw new TransformerException(this,e);
		}
		return message;
	}

}
