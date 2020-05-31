package nl.clockwork.ebms.jms;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import lombok.val;

public class JmsTemplateFactory
{
	private static Map<String,JmsTemplate> jmsTemplates = new HashMap<String,JmsTemplate>();

	public static JmsTemplate getInstance(String jmsBrokerUrl)
	{
		if (!jmsTemplates.containsKey(jmsBrokerUrl))
		{
			val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
			JmsTemplate jmsTemplate = new JmsTemplate(pooledConnectionFactory);
			jmsTemplates.put(jmsBrokerUrl,jmsTemplate);
		}
		return jmsTemplates.get(jmsBrokerUrl);
	}
}
