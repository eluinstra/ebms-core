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

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.jms.JMSDestinationType;
import nl.clockwork.ebms.jms.JMSUtils;

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
	public EventListenerFactory(@NonNull EventListenerType type, @NonNull EbMSDAO ebMSDAO, @NonNull EbMSMessageEventDAO ebMSMessageEventDAO, @NonNull String jmsBrokerURL, JMSDestinationType jmsDestinationType) throws Exception
	{
		switch (type)
		{
			case DAO:
				listener = new DAOEventListener(ebMSMessageEventDAO);
				break;
			case SIMPLE_JMS:
				listener = new SimpleJMSEventListener(JMSUtils.createJmsTemplate(jmsBrokerURL),JMSUtils.createDestinations(jmsDestinationType));
				break;
			case JMS:
				listener = new JMSEventListener(ebMSDAO,JMSUtils.createJmsTemplate(jmsBrokerURL),JMSUtils.createDestinations(jmsDestinationType));
				break;
			case JMS_TEXT:
				listener = new JMSTextEventListener(ebMSDAO,JMSUtils.createJmsTemplate(jmsBrokerURL),JMSUtils.createDestinations(jmsDestinationType));
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

}
