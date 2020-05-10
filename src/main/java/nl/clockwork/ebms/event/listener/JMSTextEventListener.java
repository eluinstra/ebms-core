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
package nl.clockwork.ebms.event.listener;

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
import nl.clockwork.ebms.service.model.EbMSMessageContext;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class JMSTextEventListener extends LoggingEventListener
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	public class EventMessageCreator implements MessageCreator
	{
		@NonNull
		EbMSMessageContext messageContext;

		@Override
		public Message createMessage(Session session) throws JMSException
		{
			val result = session.createTextMessage();
			result.setStringProperty("cpaId",messageContext.getCpaId());
			result.setStringProperty("fromPartyId",messageContext.getFromParty().getPartyId());
			result.setStringProperty("fromRole",messageContext.getFromParty().getRole());
			result.setStringProperty("toPartyId",messageContext.getToParty().getPartyId());
			result.setStringProperty("toRole",messageContext.getToParty().getRole());
			result.setStringProperty("service",messageContext.getService());
			result.setStringProperty("action",messageContext.getAction());
			result.setStringProperty("conversationId",messageContext.getConversationId());
			result.setStringProperty("messageId",messageContext.getMessageId());
			result.setStringProperty("refToMessageId",messageContext.getRefToMessageId());
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
	public void onMessageReceived(String messageId) throws EventException
	{
		try
		{
			ebMSDAO.getMessageContext(messageId)
					.ifPresent(mc -> jmsTemplate.send(destinations.get(EbMSMessageEventType.RECEIVED.name()),new EventMessageCreator(mc)));
			super.onMessageReceived(messageId);
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		try
		{
			ebMSDAO.getMessageContext(messageId)
					.ifPresent(mc -> jmsTemplate.send(destinations.get(EbMSMessageEventType.DELIVERED.name()),new EventMessageCreator(mc)));
			super.onMessageDelivered(messageId);
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}
	
	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		try
		{
			ebMSDAO.getMessageContext(messageId)
					.ifPresent(mc -> jmsTemplate.send(destinations.get(EbMSMessageEventType.FAILED.name()),new EventMessageCreator(mc)));
			super.onMessageFailed(messageId);
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		try
		{
			ebMSDAO.getMessageContext(messageId)
					.ifPresent(mc -> jmsTemplate.send(destinations.get(EbMSMessageEventType.EXPIRED.name()),new EventMessageCreator(mc)));
			super.onMessageExpired(messageId);
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}
}
