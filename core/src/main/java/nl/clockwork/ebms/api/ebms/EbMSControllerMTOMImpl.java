/*
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
package nl.clockwork.ebms.api.ebms;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.api.ebms.model.MTOMMessage;
import nl.clockwork.ebms.api.ebms.model.MTOMMessageRequest;
import nl.clockwork.ebms.api.ebms.model.MessageEvent;
import nl.clockwork.ebms.api.ebms.model.MessageFilter;
import nl.clockwork.ebms.api.ebms.model.MessageStatus;
import nl.clockwork.ebms.common.event.MessageEventType;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSControllerMTOMImpl implements EbMSControllerMTOM
{
	EbMSControllerHandler serviceHandler;

	@Override
	public void ping(String cpaId, String fromPartyId, String toPartyId) throws EbMSControllerException
	{
		try
		{
			serviceHandler.ping(cpaId, fromPartyId, toPartyId);
		}
		catch (RuntimeException e)
		{
			log.error("Ping " + cpaId, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public String sendMessageMTOM(MTOMMessageRequest message) throws EbMSControllerException
	{
		try
		{
			return serviceHandler.sendMessageMTOM(message);
		}
		catch (SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerException | RuntimeException e)
		{
			log.error("SendMessage " + message, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSControllerException
	{
		try
		{
			return serviceHandler.resendMessage(messageId);
		}
		catch (RuntimeException e)
		{
			log.error("ResendMessage {}", messageId);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public List<String> getUnprocessedMessageIds(MessageFilter messageFilter, Integer maxNr) throws EbMSControllerException
	{
		try
		{
			return serviceHandler.getUnprocessedMessageIds(messageFilter, maxNr);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessageIds " + messageFilter, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public MTOMMessage getMessageMTOM(String messageId, Boolean process) throws EbMSControllerException
	{
		try
		{
			return serviceHandler.getMessageMTOM(messageId, process);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessage " + messageId, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public void processMessage(String messageId) throws EbMSControllerException
	{
		try
		{
			serviceHandler.processMessage(messageId);
		}
		catch (RuntimeException e)
		{
			log.error("ProcessMessage " + messageId, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSControllerException
	{
		try
		{
			return serviceHandler.getMessageStatus(messageId);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessageStatus " + messageId, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, MessageEventType[] eventTypes, Integer maxNr)
			throws EbMSControllerException
	{
		try
		{
			return serviceHandler.getUnprocessedMessageEvents(messageFilter, eventTypes, maxNr);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessageEvents" + messageFilter, e);
			throw new EbMSControllerException(e);
		}
	}

	@Override
	public void processMessageEvent(String messageId) throws EbMSControllerException
	{
		try
		{
			serviceHandler.processMessageEvent(messageId);
		}
		catch (RuntimeException e)
		{
			log.error("ProcessMessageEvent " + messageId, e);
			throw new EbMSControllerException(e);
		}
	}
}
