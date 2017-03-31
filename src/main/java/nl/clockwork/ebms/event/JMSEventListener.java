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
package nl.clockwork.ebms.event;

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import nl.clockwork.ebms.Constants.EbMSMessageEventType;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class JMSEventListener implements EventListener
{
	public class EventMessageCreator implements MessageCreator
	{
		private EbMSMessageContext messageContext;

		public EventMessageCreator(EbMSMessageContext messageContext)
		{
			this.messageContext = messageContext;
		}

		@Override
		public Message createMessage(Session session) throws JMSException
		{
			Message result = session.createMessage();
			result.setStringProperty("cpaId",messageContext.getCpaId());
			result.setStringProperty("fromPartyId",messageContext.getFromRole().getPartyId());
			result.setStringProperty("fromRole",messageContext.getFromRole().getRole());
			result.setStringProperty("toPartyId",messageContext.getToRole().getPartyId());
			result.setStringProperty("toRole",messageContext.getToRole().getRole());
			result.setStringProperty("service",messageContext.getService());
			result.setStringProperty("action",messageContext.getAction());
			result.setStringProperty("conversationId",messageContext.getConversationId());
			result.setStringProperty("messageId",messageContext.getMessageId());
			result.setStringProperty("refToMessageId",messageContext.getRefToMessageId());
			return result;
		}
	}

	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private JmsTemplate jmsTemplate;
	private Map<String,Destination> destinations;

	public JMSEventListener()
	{
	}

	public JMSEventListener(EbMSDAO ebMSDAO, JmsTemplate jmsTemplate, Map<String,Destination> destinations)
	{
		this.ebMSDAO = ebMSDAO;
		this.jmsTemplate = jmsTemplate;
		this.destinations = destinations;
	}

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " received");
			jmsTemplate.send(destinations.get(EbMSMessageEventType.RECEIVED.name()),new EventMessageCreator(ebMSDAO.getMessageContext(messageId)));
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}

	@Override
	public void onMessageAcknowledged(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " acknowledged");
			jmsTemplate.send(destinations.get(EbMSMessageEventType.ACKNOWLEDGED.name()),new EventMessageCreator(ebMSDAO.getMessageContext(messageId)));
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
			logger.info("Message " + messageId + " failed");
			jmsTemplate.send(destinations.get(EbMSMessageEventType.FAILED.name()),new EventMessageCreator(ebMSDAO.getMessageContext(messageId)));
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
			logger.info("Message " + messageId + " expired");
			jmsTemplate.send(destinations.get(EbMSMessageEventType.EXPIRED.name()),new EventMessageCreator(ebMSDAO.getMessageContext(messageId)));
		}
		catch (JmsException e)
		{
			throw new EventException(e);
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate)
	{
		this.jmsTemplate = jmsTemplate;
	}
	
	public void setDestinations(Map<String,Destination> destinations)
	{
		this.destinations = destinations;
	}
}
