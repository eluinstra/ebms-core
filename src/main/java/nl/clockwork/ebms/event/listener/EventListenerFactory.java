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

import java.io.IOException;
import java.util.HashMap;

import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.dao.EbMSMessageEventDAO;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventListenerFactory implements FactoryBean<EventListener>, DisposableBean
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT;
	}
	
	@NonNull
	EventListener listener;
	@NonFinal
	BrokerFactoryBean brokerFactoryBean;

	@Builder(setterPrefix = "set")
	public EventListenerFactory(@NonNull EventListenerType type, @NonNull EbMSDAO ebMSDAO, @NonNull EbMSMessageEventDAO ebMSMessageEventDAO, @NonNull String jmsBrokerConfig, boolean jmsBrokerStart, @NonNull String jmsBrokerURL, boolean jmsVirtualTopics) throws Exception
	{
		switch (type)
		{
			case DAO:
				listener = new DAOEventListener(ebMSMessageEventDAO);
				break;
			case SIMPLE_JMS:
				startJMSBroker(jmsBrokerStart,jmsBrokerConfig);
				listener = new SimpleJMSEventListener(createJmsTemplate(jmsBrokerURL),createDestinations(jmsVirtualTopics));
				break;
			case JMS:
				startJMSBroker(jmsBrokerStart,jmsBrokerConfig);
				listener = new JMSEventListener(ebMSDAO,createJmsTemplate(jmsBrokerURL),createDestinations(jmsVirtualTopics));
				break;
			case JMS_TEXT:
				startJMSBroker(jmsBrokerStart,jmsBrokerConfig);
				listener = new JMSTextEventListener(ebMSDAO,createJmsTemplate(jmsBrokerURL),createDestinations(jmsVirtualTopics));
				break;
			default:
				listener = new LoggingEventListener();
		}
	}

	@Override
	public EventListener getObject() throws Exception
	{
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
		val pooledConnectionFactory = createConnectionFactory(jmsBrokerURL);
		val jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(pooledConnectionFactory);
		return jmsTemplate;
	}

	private PooledConnectionFactory createConnectionFactory(String jmsBrokerURL)
	{
		val result = new PooledConnectionFactory();
		result.setConnectionFactory(createActiveMQConnectionFactory(jmsBrokerURL));
		return result;
	}

	private ActiveMQConnectionFactory createActiveMQConnectionFactory(String jmsBrokerURL)
	{
		val result = new ActiveMQConnectionFactory();
		result.setBrokerURL(jmsBrokerURL);
		return result;
	}

	private HashMap<String,Destination> createDestinations(boolean jmsVirtualTopics)
	{
		val result = new HashMap<String,Destination>();
		EbMSMessageEventType.stream().forEach(e -> result.put(e.name(),jmsVirtualTopics ? new ActiveMQTopic("VirtualTopic." + e.name()) : new ActiveMQQueue(e.name())));
		return result;
	}

	private void startJMSBroker(boolean jmsBrokerStart, String jmsBrokerConfig) throws Exception
	{
		if (jmsBrokerStart && brokerFactoryBean != null)
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
}
