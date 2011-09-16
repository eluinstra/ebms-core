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

import java.util.Comparator;
import java.util.List;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSChannel;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageIdToEbMSDelegateEndpointEnricher extends AbstractMessageAwareTransformer
{
	private Comparator<PartyId> partyIdComparator =
		new Comparator<PartyId>()
		{
			@Override
			public int compare(PartyId partyId1, PartyId partyId2)
			{
				int result = 0;
				return (result = partyId1.getType().compareTo(partyId2.getType())) == 0 ? partyId1.getValue().compareTo(partyId2.getValue()) : result;
			}
		}
	; 
	private EbMSDAO ebMSDAO;
	private List<EbMSChannel> channels;

	public EbMSMessageIdToEbMSDelegateEndpointEnricher()
	{
		//registerSourceType(EbMSMessage.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			long messageId = message.getLongProperty(Constants.EBMS_MESSAGE_ID,0);
			MessageHeader messageHeader = ebMSDAO.getMessageHeader(messageId);
			//TODO get channels from database
			for (EbMSChannel channel : channels)
				if (equalsChannel(channel,messageHeader))
				{
					message.setProperty("delegate.path",channel.getEndpoint());
					break;
				}
			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	private boolean equalsChannel(EbMSChannel channel, MessageHeader messageHeader)
	{
		return channel.getCpaId().equals(messageHeader.getCPAId())
			&& channel.getFrom().getRole().equals(messageHeader.getFrom().getRole())
			//FIXME
			//&& channel.getFrom().getPartyId().equals(messageHeader.getFrom().getPartyId())
			&& partyIdComparator.compare(channel.getFrom().getPartyId().get(0),messageHeader.getFrom().getPartyId().get(0)) == 0
			&& channel.getTo().getRole().equals(messageHeader.getTo().getRole())
			//FIXME
			//&& channel.getTo().getPartyId().equals(messageHeader.getTo().getPartyId())
			&& partyIdComparator.compare(channel.getFrom().getPartyId().get(0),messageHeader.getFrom().getPartyId().get(0)) == 0
			&& channel.getService().getType().equals(messageHeader.getService().getType())
			&& channel.getService().getValue().equals(messageHeader.getService().getValue())
			&& channel.getAction().equals(messageHeader.getAction())
		;
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setChannels(List<EbMSChannel> channels)
	{
		this.channels = channels;
	}
}
