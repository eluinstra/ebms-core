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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.bridge.tcp.model.EbMSDataSource;
import nl.clockwork.mule.ebms.bridge.tcp.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;

import org.apache.commons.io.IOUtils;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageContentSerializer extends AbstractMessageAwareTransformer
{
	public EbMSMessageContentSerializer()
	{
		registerSourceType(EbMSMessageContent.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessageContent content = (EbMSMessageContent)message.getPayload();
			
			List<EbMSDataSource> attachments = new ArrayList<EbMSDataSource>();
			for (DataSource dataSource : content.getAttachments())
				attachments.add(new EbMSDataSource(dataSource.getName(),IOUtils.toByteArray(dataSource.getInputStream()),dataSource.getContentType()));

			message.setPayload(new nl.clockwork.mule.ebms.bridge.tcp.model.EbMSMessageContent(content.getContext() == null ? null : new EbMSMessageContext(content.getContext().getConversationId(),content.getContext().getMessageId()),content.getProperties(),attachments));
			return message;
		}
		catch (IOException e)
		{
			throw new TransformerException(this,e);
		}
	}

}
