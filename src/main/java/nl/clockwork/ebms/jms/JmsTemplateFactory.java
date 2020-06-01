package nl.clockwork.ebms.jms;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import lombok.val;

public class JmsTemplateFactory
{
	public static JmsTemplate getInstance(String jmsBrokerUrl)
	{
		val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
		return new JmsTemplate(pooledConnectionFactory);
	}
}
