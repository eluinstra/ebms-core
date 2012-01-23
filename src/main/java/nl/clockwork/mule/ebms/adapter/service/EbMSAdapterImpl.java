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

import java.util.ArrayList;
import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageContent;
import nl.clockwork.mule.ebms.model.EbMSMessageContext;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSAdapterImpl implements EbMSAdapter
{
  protected transient Log logger = LogFactory.getLog(getClass());
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
			//throw new EbMSAdapterException(e);
			logger.warn("",e);
			return null;
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr)
	{
		try
		{
			//FIXME use messageContext
			if (maxNr == null)
				return ebMSDAO.getMessageIds();
			else
				return ebMSDAO.getMessageIds(maxNr);
		}
		catch (DAOException e)
		{
			//throw new EbMSAdapterException(e);
			logger.warn("",e);
			return new ArrayList<String>();
		}
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, Boolean process)
	{
		try
		{
			EbMSBaseMessage message = ebMSDAO.getEbMSMessage(messageId);
			if (message instanceof EbMSMessage)
			{
				EbMSMessageContent result = EbMSMessageUtils.EbMSMessageToEbMSMessageContent((EbMSMessage)message);
				if (process != null && process)
					ebMSDAO.processMessage(messageId);
				return result;
			}
			return null;
		}
		catch (Exception e)
		{
			//throw new EbMSAdapterException(e);
			logger.warn("",e);
			return null;
		}
	}

	@Override
	public boolean processMessage(String messageId)
	{
		try
		{
			ebMSDAO.processMessage(messageId);
			return true;
		}
		catch (DAOException e)
		{
			//throw new EbMSAdapterException(e);
			logger.warn("",e);
			return false;
		}
	}

	@Override
	public boolean processMessages(List<String> messageIds)
	{
		try
		{
			ebMSDAO.processMessages(messageIds);
			return true;
		}
		catch (DAOException e)
		{
			//throw new EbMSAdapterException(e);
			logger.warn("",e);
			return false;
		}
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
