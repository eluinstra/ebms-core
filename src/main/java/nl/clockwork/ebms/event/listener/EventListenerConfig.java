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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListenerFactory.EventListenerType;
import nl.clockwork.ebms.jms.JMSDestinationType;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventListenerConfig
{
	@Value("${eventListener.type}")
	EventListenerType eventListenerType;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	EbMSMessageEventDAO ebMSMessageEventDAO;
	@Autowired
	JmsTemplate jmsTemplate;
	@Value("${jms.destinationType}")
	JMSDestinationType jmsDestinationType;

	@Bean
	public EventListener eventListener() throws Exception
	{
		return EventListenerFactory.builder()
				.setType(eventListenerType)
				.setEbMSDAO(ebMSDAO)
				.setEbMSMessageEventDAO(ebMSMessageEventDAO)
				.setJmsTemplate(jmsTemplate)
				.setJmsDestinationType(jmsDestinationType)
				.build()
				.getObject();
	}
}
