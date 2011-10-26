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
package nl.clockwork.mule.ebms.bridge.tcp.transformer;

import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import nl.clockwork.mule.ebms.bridge.tcp.model.EbMSDataSource;
import nl.clockwork.mule.ebms.bridge.tcp.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageContentDeserializer extends AbstractMessageAwareTransformer
{
	public EbMSMessageContentDeserializer()
	{
		registerSourceType(EbMSMessageContent.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
		
		List<DataSource> attachments = new ArrayList<DataSource>();
		for (EbMSDataSource dataSource : content.getAttachments())
		{
			ByteArrayDataSource ds = new ByteArrayDataSource(dataSource.getContent(),dataSource.getContentType());
			ds.setName(dataSource.getName());
			attachments.add(ds);
		}
		
		message.setPayload(new nl.clockwork.mule.ebms.model.EbMSMessageContent(content.getContext() == null ? null : new EbMSMessageContext(content.getContext().getConversationId()),content.getProperties(),attachments));
		return message;
	}

}
