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

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JMSEventListener implements EventListener
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private Connection connection;

	public JMSEventListener() throws JMSException
	{
		this("vm:broker:(tcp://localhost:61616)?brokerName=localhost&persistent=true&dataDirectory=data&useJmx=true");
	}
	
	public JMSEventListener(String brokerURL) throws JMSException
	{
		QueueConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerURL);
		connection = connectionFactory.createConnection();
		connection.start();
	}

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " received");
			EbMSMessageContext messageContext = ebMSDAO.getMessageContext(messageId);
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = session.createProducer(session.createQueue(Constants.EVENT_RECEIVED));
			producer.send(createMessage(session,messageContext));
		}
		catch (JMSException e)
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
			EbMSMessageContext messageContext = ebMSDAO.getMessageContext(messageId);
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = session.createProducer(session.createQueue(Constants.EVENT_ACKNOWLEDGED));
			producer.send(createMessage(session,messageContext));
		}
		catch (JMSException e)
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
			EbMSMessageContext messageContext = ebMSDAO.getMessageContext(messageId);
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = session.createProducer(session.createQueue(Constants.EVENT_FAILED));
			producer.send(createMessage(session,messageContext));
		}
		catch (JMSException e)
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
			EbMSMessageContext messageContext = ebMSDAO.getMessageContext(messageId);
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			MessageProducer producer = session.createProducer(session.createQueue(Constants.EVENT_EXPIRED));
			producer.send(createMessage(session,messageContext));
		}
		catch (JMSException e)
		{
			throw new EventException(e);
		}
	}

	private Message createMessage(Session session, EbMSMessageContext messageContext) throws JMSException
	{
		Message message = session.createMessage();
		message.setStringProperty("cpaId",messageContext.getCpaId());
		message.setStringProperty("fromPartyId",messageContext.getFromRole().getPartyId());
		message.setStringProperty("fromRole",messageContext.getFromRole().getRole());
		message.setStringProperty("toPartyId",messageContext.getToRole().getPartyId());
		message.setStringProperty("toRole",messageContext.getToRole().getRole());
		message.setStringProperty("service",messageContext.getService());
		message.setStringProperty("action",messageContext.getAction());
		message.setStringProperty("conversationId",messageContext.getConversationId());
		message.setStringProperty("messageId",messageContext.getMessageId());
		message.setStringProperty("refToMessageId",messageContext.getRefToMessageId());
		if (messageContext.getSequenceNr() != null)
			message.setIntProperty("sequenceNr",messageContext.getSequenceNr());
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
