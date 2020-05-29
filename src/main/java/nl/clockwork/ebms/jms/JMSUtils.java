package nl.clockwork.ebms.jms;

import java.util.HashMap;

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

	public static HashMap<String,Destination> createDestinations(boolean jmsVirtualTopics)
	{
		val result = new HashMap<String,Destination>();
		EbMSMessageEventType.stream().forEach(e -> result.put(e.name(),jmsVirtualTopics ? new ActiveMQTopic("VirtualTopic." + e.name()) : new ActiveMQQueue(e.name())));
		return result;
	}
}
