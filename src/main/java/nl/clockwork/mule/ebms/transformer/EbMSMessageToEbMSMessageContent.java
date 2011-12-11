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
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

import org.apache.commons.jxpath.JXPathContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToEbMSMessageContent extends AbstractMessageAwareTransformer
{
	private Map<String,String> properties = new HashMap<String,String>();

	public EbMSMessageToEbMSMessageContent()
	{
		registerSourceType(EbMSMessage.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		EbMSMessage msg = (EbMSMessage)message.getPayload();
		MessageHeader messageHeader = msg.getMessageHeader();
		List<DataSource> attachments = msg.getAttachments();

		Map<String,Object> properties = new HashMap<String,Object>();
		JXPathContext context = JXPathContext.newContext(messageHeader);
		for (String property : this.properties.keySet())
			properties.put(property,context.getValue(this.properties.get(property)));

		message.setPayload(new EbMSMessageContent(new EbMSMessageContext(messageHeader),properties,attachments));
		return message;
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
