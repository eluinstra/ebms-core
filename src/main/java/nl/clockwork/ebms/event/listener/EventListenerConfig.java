package nl.clockwork.ebms.event.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	@Value("${jms.brokerURL}")
	String jmsBrokerURL;
	@Value("${jms.destinationType}")
	JMSDestinationType jmsDestinationType;

	@Bean
	public EventListener eventListener() throws Exception
	{
		return EventListenerFactory.builder()
				.setType(eventListenerType)
				.setEbMSDAO(ebMSDAO)
				.setEbMSMessageEventDAO(ebMSMessageEventDAO)
				.setJmsBrokerURL(jmsBrokerURL)
				.setJmsDestinationType(jmsDestinationType)
				.build()
				.getObject();
	}
}
