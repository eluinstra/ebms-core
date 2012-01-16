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
package nl.clockwork.mule.ebms.adapter.service;

import java.util.List;

import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

public class EbMSAdapterImpl implements EbMSAdapter
{
	private EbMSDAO ebMSDAO;
	private String hostname;

	@Override
	public String sendMessage(EbMSMessageContent messageContent)
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageContent.getContext().getCpaId());
			EbMSMessage message = EbMSMessageUtils.ebMSMessageContentToEbMSMessage(cpa,messageContent,hostname);
			ebMSDAO.insertMessage(message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	@Override
	public List<String> getMessageIds(int maxNr)
	{
		return null;
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, boolean process)
	{
		return null;
	}

	@Override
	public boolean processMessage(String messageId)
	{
		return false;
	}

	@Override
	public boolean processMessages(List<String> messageIds)
	{
		return false;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
}
