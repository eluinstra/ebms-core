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
package nl.clockwork.mule.ebms.channel;

import java.util.List;

import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class XMLChannelManager implements ChannelManager
{
	private List<XMLChannel> channels;

	@Override
	public Channel getChannel(MessageHeader messageHeader) throws Exception
	{
		for (XMLChannel channel : channels)
			if (channel.getService().equals(messageHeader.getService().getValue())
				&& channel.getFrom().equals(messageHeader.getFrom().getRole())
				&& channel.getTo().equals(messageHeader.getTo().getRole())
				&& channel.getAction().equals(messageHeader.getAction())
			)
				return null;//channel;
		return null;
	}

	@Override
	public Channel getChannel(EbMSMessageContext messageContext, String channelId) throws Exception
	{
		return null; //channels.;
	}

	public void setChannels(List<XMLChannel> channels)
	{
		this.channels = channels;
	}
}
