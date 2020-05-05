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

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;
import nl.clockwork.ebms.service.model.MessageStatus;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.validation.ValidatorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageServiceMTOMImpl implements EbMSMessageServiceMTOM
{
	EbMSMessageServiceImpl ebMSMessageService;

	@Override
	public void ping(String cpaId, Party fromParty, Party toParty) throws EbMSMessageServiceException
	{
		ebMSMessageService.ping(cpaId,fromParty,toParty);
	}

	@Override
	public String sendMessageMTOM(EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageService.ebMSMessageContextValidator.validate(messageContent.getContext());
			val message = ebMSMessageService.ebMSMessageFactory.createEbMSMessageMTOM(messageContent);
			val document = EbMSMessageUtils.getEbMSDocument(message);
			ebMSMessageService.signatureGenerator.generate(document,message);
			ebMSMessageService.storeMessage(document.getMessage(),message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		return ebMSMessageService.resendMessage(messageId);
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getMessageIds(messageContext,maxNr);
	}

	@Override
	public EbMSMessageContentMTOM getMessageMTOM(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			if (process != null && process)
				processMessage(messageId);
			return ebMSMessageService.ebMSDAO.getMessageContentMTOM(messageId).orElse(null);
		}
		catch (DAOException e)
		{
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
	public MessageStatus getMessageStatus(String cpaId, Party fromParty, Party toParty, String messageId) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getMessageStatus(cpaId,fromParty,toParty,messageId);
	}

	@Override
	public List<EbMSMessageEvent> getMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		return ebMSMessageService.getMessageEvents(messageContext,eventTypes,maxNr);
	}

	@Override
	public void processMessageEvent(String messageId) throws EbMSMessageServiceException
	{
		ebMSMessageService.processMessageEvent(messageId);
	}
}
