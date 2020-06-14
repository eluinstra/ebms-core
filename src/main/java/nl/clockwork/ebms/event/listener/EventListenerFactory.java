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
import java.util.stream.Collectors;

import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.jms.JMSDestinationType;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventListenerFactory implements FactoryBean<EventListener>
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT;
	}
	
	@NonNull
	EventListener listener;

	@Builder(setterPrefix = "set")
	public EventListenerFactory(
			@NonNull EventListenerType type,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull EbMSMessageEventDAO ebMSMessageEventDAO,
			@NonNull JmsTemplate jmsTemplate,
			JMSDestinationType jmsDestinationType) throws Exception
	{
		switch (type)
		{
			case DAO:
				listener = new DAOEventListener(ebMSMessageEventDAO);
				break;
			case SIMPLE_JMS:
				listener = new SimpleJMSEventListener(jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
				break;
			case JMS:
				listener = new JMSEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
				break;
			case JMS_TEXT:
				listener = new JMSTextEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
				break;
			default:
				listener = new LoggingEventListener();
		}
	}

	public static Map<String,Destination> createEbMSMessageEventDestinations(JMSDestinationType jmsDestinationType)
	{
		return EbMSMessageEventType.stream()
				.collect(Collectors.toMap(e -> e.name(),e -> jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name())));
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

}
