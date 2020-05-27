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

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;
import nl.clockwork.ebms.service.model.MessageStatus;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageServiceMTOMImpl implements EbMSMessageServiceMTOM
{
	EbMSMessageServiceImpl ebMSMessageService;

	@Override
	public void ping(String cpaId, String fromPartyId, String toPartyId) throws EbMSMessageServiceException
	{
		ebMSMessageService.ping(cpaId,fromPartyId,toPartyId);
	}

	@Override
	public String sendMessageMTOM(EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("SendMessage");
			ebMSMessageService.ebMSMessageContextValidator.validate(messageContent.getContext());
			val message = ebMSMessageService.ebMSMessageFactory.createEbMSMessageMTOM(messageContent);
			val document = EbMSMessageUtils.getEbMSDocument(message);
			ebMSMessageService.signatureGenerator.generate(document,message);
			ebMSMessageService.storeMessage(document.getMessage(),message);
			String result = message.getMessageHeader().getMessageData().getMessageId();
			log.info("Sending message " + result);
			return result;
		}
		catch (Exception e)
		{
			log.error("SendMessage " + messageContent,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		return ebMSMessageService.resendMessage(messageId);
	}

	@Override
	public List<String> getUnprocessedMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getUnprocessedMessageIds(messageContext,maxNr);
	}

	@Override
	public EbMSMessageContentMTOM getMessageMTOM(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			log.debug("GetMessage " + messageId);
			if (process != null && process)
				processMessage(messageId);
			return ebMSMessageService.ebMSDAO.getMessageContentMTOM(messageId).orElse(null);
		}
		catch (DAOException e)
		{
			log.error("GetMessage " + messageId,e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessage(String messageId) throws EbMSMessageServiceException
	{
		ebMSMessageService.processMessage(messageId);
	}

	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getMessageStatus(messageId);
	}

	@Override
	public List<EbMSMessageEvent> getUnprocessedMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getUnprocessedMessageEvents(messageContext,eventTypes,maxNr);
	}

	@Override
	public void processMessageEvent(String messageId) throws EbMSMessageServiceException
	{
		ebMSMessageService.processMessageEvent(messageId);
	}
}
