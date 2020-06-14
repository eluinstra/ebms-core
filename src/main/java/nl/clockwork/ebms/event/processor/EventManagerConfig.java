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
