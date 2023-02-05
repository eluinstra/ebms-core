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
package nl.clockwork.ebms.service;


import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageStatus;

@Slf4j
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageServiceImpl implements EbMSMessageService
{
	@NonNull
	EbMSMessageServiceHandler serviceHandler;

	@Override
	public void ping(String cpaId, String fromPartyId, String toPartyId) throws EbMSMessageServiceException
	{
		try
		{
			serviceHandler.ping(cpaId, fromPartyId, toPartyId);
		}
		catch (Exception e)
		{
			log.error("Ping " + cpaId, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String sendMessage(MessageRequest messageRequest) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.sendMessage(messageRequest);
		}
		catch (Exception e)
		{
			log.error("SendMessage " + messageRequest, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.resendMessage(messageId);
		}
		catch (Exception e)
		{
			log.error("ResendMessage {}", messageId);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<String> getUnprocessedMessageIds(MessageFilter messageFilter, Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getUnprocessedMessageIds(messageFilter, maxNr);
		}
		catch (Exception e)
		{
			log.error("GetMessageIds " + messageFilter, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public Message getMessage(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getMessage(messageId, process);
		}
		catch (Exception e)
		{
			log.error("GetMessage " + messageId, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessage(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			serviceHandler.processMessage(messageId);
		}
		catch (Exception e)
		{
			log.error("ProcessMessage " + messageId, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getMessageStatus(messageId);
		}
		catch (Exception e)
		{
			log.error("GetMessageStatus " + messageId, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, MessageEventType[] eventTypes, Integer maxNr)
			throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getUnprocessedMessageEvents(messageFilter, eventTypes, maxNr);
		}
		catch (Exception e)
		{
			log.error("GetMessageEvents" + messageFilter, e);
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public void processMessageEvent(String messageId) throws EbMSMessageServiceException
	{
		try
		{
			serviceHandler.processMessageEvent(messageId);
		}
		catch (Exception e)
		{
			log.error("ProcessMessageEvent " + messageId, e);
			throw new EbMSMessageServiceException(e);
		}
	}
}
