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

import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.util.EbMSMessageContextValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSMessageServiceImpl implements EbMSMessageService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private DeliveryManager deliveryManager;
	private EbMSDAO ebMSDAO;

	@Override
	public void ping(String cpaId, String fromParty, String toParty) throws EbMSMessageServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(cpaId);
			EbMSMessage ping = EbMSMessageUtils.createEbMSPing(cpa,fromParty,toParty);
			EbMSDocument document = deliveryManager.sendMessage(cpa,ping);
			if (document != null)
			{
				EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document.getMessage(),document.getAttachments());
				if (!EbMSAction.PONG.action().equals(message.getMessageHeader().getAction()))
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (Exception e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
	
	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		try
		{
			new EbMSMessageContextValidator(ebMSDAO).validate(messageContent.getContext());
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageContent.getContext().getCpaId());
			final EbMSMessage message = EbMSMessageUtils.ebMSMessageContentToEbMSMessage(cpa,messageContent);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						long id = ebMSDAO.insertMessage(new Date(),message,null);
						List<EbMSSendEvent> sendEvents = EbMSMessageUtils.getEbMSSendEvents(ebMSDAO.getCPA(message.getMessageHeader().getCPAId()),id,message.getMessageHeader());
						ebMSDAO.insertSendEvents(sendEvents);
					}
				}
			);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (Exception e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null)
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED);
			else
				return ebMSDAO.getMessageIds(messageContext,EbMSMessageStatus.RECEIVED,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public EbMSMessageContent getMessage(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			EbMSMessage message = ebMSDAO.getMessage(messageId);
			if (message instanceof EbMSMessage)
			{
				EbMSMessageContent result = EbMSMessageUtils.EbMSMessageToEbMSMessageContent((EbMSMessage)message);
				if (process != null && process)
					ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
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
	public void processMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessages(List<String> messageIds) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.updateMessages(messageIds,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
	
	@Override
	public MessageStatus getMessageStatus(String cpaId, String fromParty, String toParty, String messageId) throws EbMSMessageServiceException
	{
		try
		{
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(cpaId);
			EbMSMessage statusRequest = EbMSMessageUtils.createEbMSStatusRequest(cpa,fromParty,toParty,messageId);
			EbMSDocument document = deliveryManager.sendMessage(cpa,statusRequest);
			if (document != null)
			{
				EbMSMessage message = EbMSMessageUtils.getEbMSMessage(document.getMessage(),document.getAttachments());
				if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()) && message.getStatusResponse() != null)
					return new MessageStatus(message.getStatusResponse().getTimestamp() == null ? null : message.getStatusResponse().getTimestamp().toGregorianCalendar().getTime(),EbMSMessageStatus.get(message.getStatusResponse().getMessageStatus()));
				else
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (Exception e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	public void setDeliveryManager(DeliveryManager deliveryManager)
	{
		this.deliveryManager = deliveryManager;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
}
