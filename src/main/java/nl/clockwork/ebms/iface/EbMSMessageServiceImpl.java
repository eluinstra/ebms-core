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
package nl.clockwork.ebms.iface;

import java.util.List;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.util.EbMSMessageContextValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSMessageServiceImpl implements EbMSMessageService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private String hostname;

	@Override
	public String sendMessage(EbMSMessageContent messageContent)
	{
		try
		{
			new EbMSMessageContextValidator(ebMSDAO).validate(messageContent.getContext());
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageContent.getContext().getCpaId());
			EbMSMessage message = EbMSMessageUtils.ebMSMessageContentToEbMSMessage(cpa,messageContent,hostname);
			List<EbMSSendEvent> sendEvents = EbMSMessageUtils.getEbMSSendEvents(ebMSDAO.getCPA(message.getMessageHeader().getCPAId()),message.getMessageHeader());
			ebMSDAO.insertMessage(message,sendEvents);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (Exception e)
		{
			throw new EbMSMessageServiceException(e);
			//logger.warn("",e);
			//return null;
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr)
	{
		try
		{
			//FIXME use messageContext
			if (maxNr == null)
				return ebMSDAO.getReceivedMessageIds(messageContext);
			else
				return ebMSDAO.getReceivedMessageIds(messageContext,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
			//logger.warn("",e);
			//return new ArrayList<String>();
		}
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, Boolean process)
	{
		try
		{
			EbMSBaseMessage message = ebMSDAO.getMessage(messageId);
			if (message instanceof EbMSMessage)
			{
				EbMSMessageContent result = EbMSMessageUtils.EbMSMessageToEbMSMessageContent((EbMSMessage)message);
				if (process != null && process)
					ebMSDAO.processReceivedMessage(messageId);
				return result;
			}
			return null;
		}
		catch (Exception e)
		{
			throw new EbMSMessageServiceException(e);
			//logger.warn("",e);
			//return null;
		}
	}

	@Override
	public boolean processMessage(String messageId)
	{
		try
		{
			ebMSDAO.processReceivedMessage(messageId);
			return true;
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
			//logger.warn("",e);
			//return false;
		}
	}

	@Override
	public boolean processMessages(List<String> messageIds)
	{
		try
		{
			ebMSDAO.processReceivedMessages(messageIds);
			return true;
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
			//logger.warn("",e);
			//return false;
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
