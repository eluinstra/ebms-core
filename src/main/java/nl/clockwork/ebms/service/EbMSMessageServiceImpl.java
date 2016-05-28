/**
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
 */
package nl.clockwork.ebms.service;

import java.util.Date;
import java.util.List;

import javax.xml.transform.TransformerFactoryConfigurationError;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.job.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

public class EbMSMessageServiceImpl implements InitializingBean, EbMSMessageService
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private DeliveryManager deliveryManager;
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;
	private EbMSMessageFactory ebMSMessageFactory;
	private EventManager eventManager;
	private EbMSMessageContextValidator ebMSMessageContextValidator;
	private EbMSSignatureGenerator signatureGenerator;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		ebMSMessageContextValidator = new EbMSMessageContextValidator(cpaManager);
	}
  
	@Override
	public void ping(String cpaId, Party fromParty, Party toParty) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(cpaId,fromParty,toParty);
			EbMSMessage request = ebMSMessageFactory.createEbMSPing(cpaId,fromParty,toParty);
			EbMSMessage response = deliveryManager.sendMessage(cpaManager.getUri(cpaId,new CacheablePartyId(request.getMessageHeader().getTo().getPartyId()),request.getMessageHeader().getTo().getRole(),CPAUtils.toString(request.getMessageHeader().getService()),request.getMessageHeader().getAction()),request);
			if (response != null)
			{
				if (!EbMSAction.PONG.action().equals(response.getMessageHeader().getAction()))
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (ValidationException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
	
	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(messageContent.getContext());
			final EbMSMessage message = ebMSMessageFactory.createEbMSMessage(messageContent.getContext().getCpaId(),messageContent);
			signatureGenerator.generate(message);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.insertMessage(new Date(),message,EbMSMessageStatus.SENT);
						eventManager.createEvent(message.getMessageHeader().getCPAId(),cpaManager.getReceiveDeliveryChannel(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getTo().getPartyId()),message.getMessageHeader().getTo().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction()).getChannelId(),message.getMessageHeader().getMessageData().getMessageId(),message.getMessageHeader().getMessageData().getTimeToLive(),message.getMessageHeader().getMessageData().getTimestamp(),cpaManager.isConfidential(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction()));
					}
				}
			);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null || maxNr == 0)
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
			EbMSMessageContent result = ebMSDAO.getMessageContent(messageId);
			if (process != null && process)
				ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
			return result;
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
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
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			EbMSMessageContext context = ebMSDAO.getMessageContext(messageId);
			if (context == null)
				throw new EbMSMessageServiceException("No message found with messageId " + messageId + "!");
			else if (Constants.EBMS_SERVICE_URI.equals(context.getService()))
				throw new EbMSMessageServiceException("Message with messageId " + messageId + " is an EbMS service message!");
			else
			{
				EbMSMessage request = ebMSMessageFactory.createEbMSStatusRequest(context.getCpaId(),cpaManager.getFromParty(context.getCpaId(),context.getFromRole(),context.getService(),context.getAction()),cpaManager.getToParty(context.getCpaId(),context.getToRole(),context.getService(),context.getAction()),messageId);
				EbMSMessage response = deliveryManager.sendMessage(cpaManager.getUri(context.getCpaId(),new CacheablePartyId(request.getMessageHeader().getTo().getPartyId()),request.getMessageHeader().getTo().getRole(),CPAUtils.toString(request.getMessageHeader().getService()),request.getMessageHeader().getAction()),request);
				if (response != null)
				{
					if (EbMSAction.STATUS_RESPONSE.action().equals(response.getMessageHeader().getAction()) && response.getStatusResponse() != null)
						return new MessageStatus(response.getStatusResponse().getTimestamp() == null ? null : response.getStatusResponse().getTimestamp(),EbMSMessageStatus.get(response.getStatusResponse().getMessageStatus()));
					else
						throw new EbMSMessageServiceException("No valid response received!");
				}
				else
					throw new EbMSMessageServiceException("No response received!");
			}
		}
		catch (DAOException | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public MessageStatus getMessageStatus(String cpaId, Party fromParty, Party toParty, String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(cpaId,fromParty,toParty);
			EbMSMessage request = ebMSMessageFactory.createEbMSStatusRequest(cpaId,fromParty,toParty,messageId);
			EbMSMessage response = deliveryManager.sendMessage(cpaManager.getUri(cpaId,new CacheablePartyId(request.getMessageHeader().getTo().getPartyId()),request.getMessageHeader().getTo().getRole(),CPAUtils.toString(request.getMessageHeader().getService()),request.getMessageHeader().getAction()),request);
			if (response != null)
			{
				if (EbMSAction.STATUS_RESPONSE.action().equals(response.getMessageHeader().getAction()) && response.getStatusResponse() != null)
					return new MessageStatus(response.getStatusResponse().getTimestamp() == null ? null : response.getStatusResponse().getTimestamp(),EbMSMessageStatus.get(response.getStatusResponse().getMessageStatus()));
				else
					throw new EbMSMessageServiceException("No valid response received!");
			}
			else
				throw new EbMSMessageServiceException("No response received!");
		}
		catch (ValidationException | DAOException | EbMSProcessorException e)
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

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setEbMSMessageFactory(EbMSMessageFactory ebMSMessageFactory)
	{
		this.ebMSMessageFactory = ebMSMessageFactory;
	}

	public void setEventManager(EventManager eventManager)
	{
		this.eventManager = eventManager;
	}

	public void setSignatureGenerator(EbMSSignatureGenerator signatureGenerator)
	{
		this.signatureGenerator = signatureGenerator;
	}

}
