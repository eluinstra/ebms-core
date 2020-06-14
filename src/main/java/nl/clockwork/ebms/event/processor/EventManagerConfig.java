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
package nl.clockwork.ebms.event.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventManagerConfig
{
	@Value("${eventManager.type}")
	EventManagerType eventManagerType;
	@Autowired
	EbMSEventDAO ebMSEventDAO;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId}")
	String serverId;
	@Autowired()
	@Qualifier("jmsTemplate")
	JmsTemplate jmsTemplate;
	@Value("${ebmsMessage.nrAutoRetries}")
	int nrAutoRetries;
	@Value("${ebmsMessage.autoRetryInterval}")
	int autoRetryInterval;

	@Bean()
	public EventManager eventManager() throws Exception
	{
		return EventManagerFactory.builder()
				.setType(eventManagerType)
				.setEbMSEventDAO(ebMSEventDAO)
				.setCpaManager(cpaManager)
				.setServerId(serverId)
				.setJmsTemplate(jmsTemplate)
				.setNrAutoRetries(nrAutoRetries)
				.setAutoRetryInterval(autoRetryInterval)
				.build()
				.getObject();
	}
}
