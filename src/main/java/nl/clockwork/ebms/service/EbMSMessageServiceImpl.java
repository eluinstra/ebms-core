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
import nl.clockwork.ebms.Constants.EbMSMessageEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.ThrowingFunction;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.job.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageAttachment;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSMessageEvent;
import nl.clockwork.ebms.model.MessageStatus;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;

public class EbMSMessageServiceImpl implements InitializingBean, EbMSMessageService
{
	private DeliveryManager deliveryManager;
	private EbMSDAO ebMSDAO;
	private CPAManager cpaManager;
	private EbMSMessageFactory ebMSMessageFactory;
	private EventManager eventManager;
	private EbMSMessageContextValidator ebMSMessageContextValidator;
	private EbMSSignatureGenerator signatureGenerator;
	protected boolean deleteEbMSAttachmentsOnMessageProcessed;

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
			return storeMessageWithEvent(messageContent);
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String sendMessageWithAttachments(EbMSMessageAttachment message) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(message.getContext());
			return storeMessageWithEvent(message.toContent());
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return ebMSDAO.getMessageContent(messageId)
					.map(ThrowingFunction.throwingFunctionWrapper(mc ->
					{
						resetMessage(mc.getContext());
						return storeMessageWithEvent(mc);
					}))
					.orElseThrow(() -> new EbMSMessageServiceException("Message not found!"));
		}
		catch (DAOException | EbMSProcessorException e)
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
	public EbMSMessageContent getMessage(final String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			if (process != null && process)
				processMessage(messageId);
			return ebMSDAO.getMessageContent(messageId).orElse(null);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessage(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					ebMSDAO.updateMessage(messageId,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
					if (deleteEbMSAttachmentsOnMessageProcessed)
						ebMSDAO.deleteAttachments(messageId);
				}
			});
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessages(final List<String> messageIds) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					ebMSDAO.updateMessages(messageIds,EbMSMessageStatus.RECEIVED,EbMSMessageStatus.PROCESSED);
					if (deleteEbMSAttachmentsOnMessageProcessed)
						ebMSDAO.deleteAttachments(messageIds);
				}
			});
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
			return ebMSDAO.getMessageContext(messageId)
					.map(ThrowingFunction.throwingFunctionWrapper(mc ->
					{
						if (Constants.EBMS_SERVICE_URI.equals(mc.getService()))
							throw new EbMSMessageServiceException("Message with messageId " + messageId + " is an EbMS service message!");
						else
						{
							EbMSMessage request = ebMSMessageFactory.createEbMSStatusRequest(mc.getCpaId(),cpaManager.getFromParty(mc.getCpaId(),mc.getFromRole(),mc.getService(),mc.getAction()),cpaManager.getToParty(mc.getCpaId(),mc.getToRole(),mc.getService(),mc.getAction()),messageId);
							EbMSMessage response = deliveryManager.sendMessage(cpaManager.getUri(mc.getCpaId(),new CacheablePartyId(request.getMessageHeader().getTo().getPartyId()),request.getMessageHeader().getTo().getRole(),CPAUtils.toString(request.getMessageHeader().getService()),request.getMessageHeader().getAction()),request);
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
					}))
					.orElseThrow(() -> new EbMSMessageServiceException("No message found with messageId " + messageId + "!"));
					
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

	@Override
	public List<EbMSMessageEvent> getMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			if (maxNr == null || maxNr == 0)
				return ebMSDAO.getEbMSMessageEvents(messageContext,eventTypes);
			else
				return ebMSDAO.getEbMSMessageEvents(messageContext,eventTypes,maxNr);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					ebMSDAO.processEbMSMessageEvent(messageId);
					processMessage(messageId);
				}
			});
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessageEvents(final List<String> messageIds) throws EbMSMessageServiceException
	{
		try
		{
			ebMSDAO.executeTransaction(new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction() throws DAOException
				{
					ebMSDAO.processEbMSMessageEvents(messageIds);
					processMessages(messageIds);
				}
			});
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	private void resetMessage(EbMSMessageContext context)
	{
		//context.setConversationId(null);
		context.setMessageId(null);
		context.setTimestamp(null);
	}

	private String storeMessageWithEvent(EbMSMessageContent messageContent) throws EbMSProcessorException
	{
		final EbMSMessage result = ebMSMessageFactory.createEbMSMessage(messageContent.getContext().getCpaId(),messageContent);
		signatureGenerator.generate(result);
		ebMSDAO.executeTransaction(new DAOTransactionCallback()
		{
			@Override
			public void doInTransaction()
			{
				Date timestamp = new Date();
				MessageHeader rmh = result.getMessageHeader();
				DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(result.getMessageHeader().getCPAId(),new CacheablePartyId(result.getMessageHeader().getTo().getPartyId()),rmh.getTo().getRole(),CPAUtils.toString(rmh.getService()),rmh.getAction());
				ebMSDAO.insertMessage(timestamp,CPAUtils.getPersistTime(timestamp,deliveryChannel),result,EbMSMessageStatus.SENDING);
				eventManager.createEvent(rmh.getCPAId(),deliveryChannel,rmh.getMessageData().getMessageId(),rmh.getMessageData().getTimeToLive(),rmh.getMessageData().getTimestamp(),cpaManager.isConfidential(rmh.getCPAId(),new CacheablePartyId(rmh.getFrom().getPartyId()),rmh.getFrom().getRole(),CPAUtils.toString(rmh.getService()),rmh.getAction()));
			}
		});
		return result.getMessageHeader().getMessageData().getMessageId();
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

	public void setDeleteEbMSAttachmentsOnMessageProcessed(boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		this.deleteEbMSAttachmentsOnMessageProcessed = deleteEbMSAttachmentsOnMessageProcessed;
	}

}
