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
package nl.clockwork.mule.ebms.enricher;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.channel.Channel;
import nl.clockwork.mule.ebms.channel.ChannelManager;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToEbMSDelegateEndpointEnricher extends AbstractMessageAwareTransformer
{
	private ChannelManager channelManager;

	public EbMSMessageToEbMSDelegateEndpointEnricher()
	{
		//registerSourceType(EbMSMessage.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessage msg = (EbMSMessage)message.getPayload();
			MessageHeader messageHeader = msg.getMessageHeader();
			Channel channel = channelManager.getChannel(messageHeader);
			message.setProperty(Constants.EBMS_DELEGATE_PATH,channel.getEndpoint());
			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	public void setChannelManager(ChannelManager channelManager)
	{
		this.channelManager = channelManager;
	}
}
