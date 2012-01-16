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
import java.util.Map;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageIdToEbMSMessageContent extends AbstractMessageAwareTransformer
{
	private EbMSDAO ebMSDAO;
	private Map<String,String> properties = new HashMap<String,String>();

	public EbMSMessageIdToEbMSMessageContent()
	{
		//registerSourceType(EbMSMessage.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			long id = message.getLongProperty(Constants.EBMS_MESSAGE_ID,0);
			EbMSMessage msg = (EbMSMessage)ebMSDAO.getEbMSMessage(id);
			message.setPayload(EbMSMessageUtils.EbMSMessageToEbMSMessageContent(msg,properties));

			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setProperties(String properties)
	{
		 String[] p = properties.split("\\s*,\\s*");
		 for (String s : p)
		 {
			 String[] t = s.split("\\s*:\\s*");
			 this.properties.put(t[0],t[1]);
		 }
	}

}
