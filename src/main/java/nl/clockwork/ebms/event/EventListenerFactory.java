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

import java.io.IOException;
import java.util.HashMap;

import javax.jms.Destination;

import nl.clockwork.ebms.Constants.EbMSMessageEventType;
import nl.clockwork.ebms.dao.EbMSDAO;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.core.JmsTemplate;

public class EventListenerFactory implements FactoryBean<EventListener>, DisposableBean
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT;
	}
	
	protected transient Log logger = LogFactory.getLog(getClass());
	private EventListenerType type;
	private EbMSDAO ebMSDAO;
	private String jmsBrokerConfig;
	private boolean jmsBrokerStart;
	private String jmsBrokerURL;
	private boolean jmsVirtualTopics;
	private BrokerFactoryBean brokerFactoryBean;

	@Override
	public EventListener getObject() throws Exception
	{
		EventListener listener = null;
		logger.info("Using EventListener " + type.name());
		switch (type)
		{
			case DAO:
				listener = new DAOEventListener(ebMSDAO);
				break;
			case SIMPLE_JMS:
				startJMSBroker(jmsBrokerConfig, jmsBrokerStart);
				listener = new SimpleJMSEventListener(createJmsTemplate(jmsBrokerURL), createDestinations());
				break;
			case JMS:
				startJMSBroker(jmsBrokerConfig, jmsBrokerStart);
				listener = new JMSEventListener(ebMSDAO, createJmsTemplate(jmsBrokerURL), createDestinations());
				break;
			case JMS_TEXT:
				startJMSBroker(jmsBrokerConfig, jmsBrokerStart);
				listener = new JMSTextEventListener(ebMSDAO, createJmsTemplate(jmsBrokerURL), createDestinations());
				break;
			default:
				listener = new LoggingEventListener();
		}
		return listener;
	}

	@Override
	public Class<?> getObjectType()
	{
		return EventListener.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	@Override
	public void destroy() throws Exception
	{
		if (brokerFactoryBean != null)
			brokerFactoryBean.destroy();
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

	private HashMap<String,Destination> createDestinations()
	{
		HashMap<String,Destination> result = new HashMap<>();
		EbMSMessageEventType.stream().forEach(e -> result.put(e.name(),jmsVirtualTopics ? new ActiveMQTopic("VirtualTopic." + e.name()) : new ActiveMQQueue(e.name())));
		return result;
	}

	private void startJMSBroker(String jmsBrokerConfig, boolean jmsBrokerStart) throws Exception
	{
		if (jmsBrokerStart)
		{
			brokerFactoryBean = new BrokerFactoryBean();
			brokerFactoryBean.setConfig(createResource(jmsBrokerConfig));
			brokerFactoryBean.setStart(true);
			brokerFactoryBean.afterPropertiesSet();
		}
	}

	private Resource createResource(String path) throws IOException
	{
		if (path.startsWith("classpath:"))
			return new ClassPathResource(path.substring("classpath:".length()));
		else if (path.startsWith("file:"))
			return new FileSystemResource(path.substring("file:".length()));
		else
			return new FileSystemResource(path);
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

	public void setJmsBrokerConfig(String jmsBrokerConfig)
	{
		this.jmsBrokerConfig = jmsBrokerConfig;
	}

	public void setJmsBrokerStart(boolean jmsBrokerStart)
	{
		this.jmsBrokerStart = jmsBrokerStart;
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
