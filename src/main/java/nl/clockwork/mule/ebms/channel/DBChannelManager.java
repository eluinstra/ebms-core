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

import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.cpp.cpa.CanReceive;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;

public class DBChannelManager implements ChannelManager
{
	private EbMSDAO ebMSDAO;

	@Override
	public Channel getChannel(MessageHeader messageHeader) throws Exception
	{
		CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getTo().getPartyId());
		CanReceive canReceive = CPAUtils.getCanReceive(partyInfo,messageHeader.getTo().getRole(),messageHeader.getService(),messageHeader.getAction());
		Channel channel = ebMSDAO.getChannel(messageHeader.getCPAId(),canReceive.getThisPartyActionBinding().getId());
		if (channel == null)
			throw new Exception("No channel found for cpaId " + messageHeader.getCPAId() + " and actionId " + canReceive.getThisPartyActionBinding().getId());
		return channel;
	}

	@Override
	public Channel getChannel(EbMSMessageContext messageContext, String channelId) throws Exception
	{
		Channel channel = ebMSDAO.getChannel(channelId);
		if (channel == null)
			throw new Exception("No channel found with id " + channelId);
		return channel;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
