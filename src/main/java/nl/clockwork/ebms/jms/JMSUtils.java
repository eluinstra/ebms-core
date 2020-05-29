package nl.clockwork.ebms.jms;

import java.util.Map;
import java.util.stream.Collectors;

import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import lombok.val;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;

public class JMSUtils
{
	public static JmsTemplate createJmsTemplate(String jmsBrokerURL)
	{
		val pooledConnectionFactory = createConnectionFactory(jmsBrokerURL);
		val jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(pooledConnectionFactory);
		return jmsTemplate;
	}

	public static PooledConnectionFactory createConnectionFactory(String jmsBrokerURL)
	{
		val result = new PooledConnectionFactory();
		result.setConnectionFactory(createActiveMQConnectionFactory(jmsBrokerURL));
		return result;
	}

	public static ActiveMQConnectionFactory createActiveMQConnectionFactory(String jmsBrokerURL)
	{
		val result = new ActiveMQConnectionFactory();
		result.setBrokerURL(jmsBrokerURL);
		return result;
	}

	public static Map<String,Destination> createDestinations(JMSDestinationType jmsDestinationType)
	{
		return EbMSMessageEventType.stream()
				.collect(Collectors.toMap(e -> e.name(),e -> jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name())));
	}
}
