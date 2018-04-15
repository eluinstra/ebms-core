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

import java.util.HashMap;

import javax.jms.Destination;

import nl.clockwork.ebms.Constants.EbMSMessageEventType;
import nl.clockwork.ebms.dao.EbMSDAO;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jms.core.JmsTemplate;

public class EventListenerFactory implements FactoryBean<EventListener>
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS;
	}
	
	protected transient Log logger = LogFactory.getLog(getClass());
	private EventListenerType type;
	private EbMSDAO ebMSDAO;
	private String jmsBrokerURL;
	private boolean jmsVirtualTopics;

	@Override
	public EventListener getObject() throws Exception
	{
		logger.info("Using EventListener " + type.name());
		if (EventListenerType.DAO.equals(type))
		{
			return new DAOEventListener(ebMSDAO);
		}
		else if (EventListenerType.SIMPLE_JMS.equals(type))
		{
			return new SimpleJMSEventListener(createJmsTemplate(jmsBrokerURL),createDestinations(true));
		}
		else if (EventListenerType.JMS.equals(type))
		{
			return new JMSEventListener(ebMSDAO,createJmsTemplate(jmsBrokerURL),createDestinations(true));
		}
		else
		{
			return new LoggingEventListener();
		}
	}

	@Override
	public Class<?> getObjectType()
	{
		return EventListener.class;
	}

	@Override
	public boolean isSingleton()
	{
		return false;
	}

	private JmsTemplate createJmsTemplate(String jmsBrokerURL)
	{
		PooledConnectionFactory pooledConnectionFactory = createConnectionFactory(jmsBrokerURL);
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(pooledConnectionFactory);
		return jmsTemplate;
	}

	private PooledConnectionFactory createConnectionFactory(String jmsBrokerURL)
	{
		PooledConnectionFactory result = new PooledConnectionFactory();
		result.setConnectionFactory(createActiveMQConnectionFactory(jmsBrokerURL));
		return result;
	}

	private ActiveMQConnectionFactory createActiveMQConnectionFactory(String jmsBrokerURL)
	{
		ActiveMQConnectionFactory result = new ActiveMQConnectionFactory();
		result.setBrokerURL(jmsBrokerURL);
		return result;
	}

	private HashMap<String,Destination> createDestinations(boolean createTopics)
	{
		HashMap<String,Destination> result = new HashMap<String,Destination>();
		// define queues or virtual-topics for all types of events
		for (EbMSMessageEventType event : nl.clockwork.ebms.Constants.EbMSMessageEventType.values())
		{
			result.put(event.name(), jmsVirtualTopics ? new ActiveMQTopic("VirtualTopic." + event.name()) : new ActiveMQQueue(event.name()) );
		}
		return result;
	}

	public void setType(String type)
	{
		try
		{
			this.type = EventListenerType.valueOf(type);
		}
		catch (IllegalArgumentException e)
		{
			this.type = EventListenerType.DEFAULT;
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setJmsBrokerURL(String jmsBrokerURL)
	{
		this.jmsBrokerURL = jmsBrokerURL;
	}

	public void setJmsVirtualTopics(boolean jmsVirtualTopics)
	{
		this.jmsVirtualTopics = jmsVirtualTopics;
	}
}
