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

import javax.jms.ConnectionFactory;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSThreadPoolExecutor;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventProcessorConfig
{
	public enum EventProcessorType
	{
		NONE, DEFAULT, JMS;
	}
	@Value("${eventProcessor.type}")
	EventProcessorType eventProcessorType;
	@Autowired
	EbMSEventDAO ebMSEventDAO;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId}")
	String serverId;
	@Value("${eventProcessor.jms.destinationName}")
	String jmsDestinationName;
	@Value("${eventProcessor.minThreads}")
	int minThreads;
	@Value("${eventProcessor.maxThreads}")
	int maxThreads;
	@Value("${eventProcessor.maxEvents}")
	int maxEvents;
	@Value("${eventProcessor.executionInterval}")
	int executionInterval;
	@Autowired
	EventListener eventListener;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	URLMapper urlMapper;
	@Autowired
	EventManager eventManager;
	@Autowired
	EbMSHttpClientFactory ebMSClientFactory;
	@Autowired
	EbMSMessageEncrypter messageEncrypter;
	@Autowired
	EbMSMessageProcessor messageProcessor;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired
	@Qualifier("jmsTransactionManager")
	PlatformTransactionManager jmsTransactionManager;
	@Value("${eventProcessor.jms.destinationName}")
	String destinationName;

	@Bean
	public Object eventProcessor() throws Exception
	{
		switch(eventProcessorType)
		{
			case NONE:
				return null;
			case JMS:
				val result = new DefaultMessageListenerContainer();
				result.setConnectionFactory(connectionFactory);
				result.setTransactionManager(jmsTransactionManager);
				result.setConcurrentConsumers(minThreads);
				result.setMaxConcurrentConsumers(maxThreads);
				result.setDestinationName(StringUtils.isEmpty(jmsDestinationName) ? JMSEventManager.JMS_DESTINATION_NAME : jmsDestinationName);
				result.setMessageListener(new EbMSSendEventListener(handleEventTaskBuilder()));
				return result;
			default:
				return EbMSEventProcessor.builder()
						.maxEvents(maxEvents)
						.executionInterval(executionInterval)
						.ebMSThreadPoolExecutor(new EbMSThreadPoolExecutor(minThreads,maxThreads))
						.ebMSEventDAO(ebMSEventDAO)
						.handleEventTaskBuilder(handleEventTaskBuilder())
						.serverId(serverId)
						.build();
		}
	}

	private HandleEventTask.HandleEventTaskBuilder handleEventTaskBuilder()
	{
		HandleEventTask.HandleEventTaskBuilder handleEventTaskBuilder = HandleEventTask.builder()
				.eventListener(eventListener)
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.urlMapper(urlMapper)
				.eventManager(eventManager)
				.ebMSClientFactory(ebMSClientFactory)
				.messageEncrypter(messageEncrypter)
				.messageProcessor(messageProcessor)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.executionInterval(executionInterval);
		return handleEventTaskBuilder;
	}
}
