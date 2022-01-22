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
package nl.clockwork.ebms.event;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageProperties;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class JMSTextMessageEventListener extends LoggingMessageEventListener
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	public class EventMessageCreator implements MessageCreator
	{
		@NonNull
		EbMSMessageProperties messageProperties;

		@Override
		public Message createMessage(Session session) throws JMSException
		{
			val result = session.createTextMessage();
			result.setStringProperty("cpaId",messageProperties.getCpaId());
			result.setStringProperty("fromPartyId",messageProperties.getFromParty().getPartyId());
			result.setStringProperty("fromRole",messageProperties.getFromParty().getRole());
			result.setStringProperty("toPartyId",messageProperties.getToParty().getPartyId());
			result.setStringProperty("toRole",messageProperties.getToParty().getRole());
			result.setStringProperty("service",messageProperties.getService());
			result.setStringProperty("action",messageProperties.getAction());
			result.setStringProperty("conversationId",messageProperties.getConversationId());
			result.setStringProperty("messageId",messageProperties.getMessageId());
			result.setStringProperty("refToMessageId",messageProperties.getRefToMessageId());
			result.setText("EbMS Message Context");
			return result;
		}
	}

	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	JmsTemplate jmsTemplate;
	@NonNull
	Map<String,Destination> destinations;

	@Override
	public void onMessageReceived(String messageId) throws MessageEventException
	{
		try
		{
			ebMSDAO.getEbMSMessageProperties(messageId)
					.ifPresent(p -> jmsTemplate.send(destinations.get(MessageEventType.RECEIVED.name()),new EventMessageCreator(p)));
			super.onMessageReceived(messageId);
		}
		catch (JmsException e)
		{
			throw new MessageEventException(e);
		}
	}

	@Override
	public void onMessageDelivered(String messageId) throws MessageEventException
	{
		try
		{
			ebMSDAO.getEbMSMessageProperties(messageId)
					.ifPresent(p -> jmsTemplate.send(destinations.get(MessageEventType.DELIVERED.name()),new EventMessageCreator(p)));
			super.onMessageDelivered(messageId);
		}
		catch (JmsException e)
		{
			throw new MessageEventException(e);
		}
	}
	
	@Override
	public void onMessageFailed(String messageId) throws MessageEventException
	{
		try
		{
			ebMSDAO.getEbMSMessageProperties(messageId)
					.ifPresent(p -> jmsTemplate.send(destinations.get(MessageEventType.FAILED.name()),new EventMessageCreator(p)));
			super.onMessageFailed(messageId);
		}
		catch (JmsException e)
		{
			throw new MessageEventException(e);
		}
	}

	@Override
	public void onMessageExpired(String messageId) throws MessageEventException
	{
		try
		{
			ebMSDAO.getEbMSMessageProperties(messageId)
					.ifPresent(p -> jmsTemplate.send(destinations.get(MessageEventType.EXPIRED.name()),new EventMessageCreator(p)));
			super.onMessageExpired(messageId);
		}
		catch (JmsException e)
		{
			throw new MessageEventException(e);
		}
	}
}
