package nl.clockwork.ebms.event.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSThreadPoolExecutor;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventProcessorConfig
{
	@Value("${eventManager.type}")
	EventManagerType eventManagerType;
	@Autowired
	EbMSEventDAO ebMSEventDAO;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId}")
	String serverId;
	@Value("${jms.brokerURL}")
	String jmsBrokerUrl;
	@Value("${eventProcessor.start}")
	boolean startEventProcessor;
	@Value("${eventProcessor.jms.destinationName}")
	String jmsDestinationName;
	@Value("${eventProcessor.maxTreads}")
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

	@Bean
	public void eventProcessor() throws Exception
	{
		if (startEventProcessor)
		{
			HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype = HandleEventTask.builder()
					.setEventListener(eventListener)
					.setEbMSDAO(ebMSDAO)
					.setCpaManager(cpaManager)
					.setUrlMapper(urlMapper)
					.setEventManager(eventManager)
					.setEbMSClientFactory(ebMSClientFactory)
					.setMessageEncrypter(messageEncrypter)
					.setMessageProcessor(messageProcessor)
					.setDeleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
					.setExecutionInterval(executionInterval);
			switch(eventManagerType)
			{
				case JMS:
					JMSEventProcessor.builder()
							.setStart(startEventProcessor)
							.setType(eventManagerType)
							.setJmsBrokerUrl(jmsBrokerUrl)
							.setJmsDestinationName(jmsDestinationName)
							.setMaxThreads(maxThreads)
							.setHandleEventTaskPrototype(handleEventTaskPrototype)
							.build();
					break;
				default:
					EbMSEventProcessor.builder()
							.setStart(startEventProcessor)
							.setType(eventManagerType)
							.setMaxEvents(maxEvents)
							.setExecutionInterval(executionInterval)
							.setEbMSThreadPoolExecutor(new EbMSThreadPoolExecutor(maxThreads))
							.setEbMSEventDAO(ebMSEventDAO)
							.setHandleEventTaskPrototype(handleEventTaskPrototype)
							.setServerId(serverId)
							.build();
			}
		}
	}
}
