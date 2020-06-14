package nl.clockwork.ebms.jms;

import javax.jms.Session;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JMSConfig
{
	@Value("${jms.broker.start}")
	boolean jmsBrokerStart;
	@Value("${jms.broker.config}")
	String jmsBrokerConfig;
	@Value("${jms.brokerURL}")
	String jmsBrokerUrl;
	@Value("${jms.pool.maxConnections}")
	int maxConnections;

	@Bean("brokerFactory")
	public void brokerFactory() throws Exception
	{
		EbMSBrokerFactoryBean.init(jmsBrokerStart,jmsBrokerConfig);
	}

	@Bean(name="jmsTemplate")
	@DependsOn("brokerFactory")
	public JmsTemplate jmsTemplate()
	{
		val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
		pooledConnectionFactory.setMaxConnections(maxConnections);
		return new JmsTemplate(pooledConnectionFactory);
	}

	@Bean(name="transactedJmsTemplate")
	@DependsOn("brokerFactory")
	public JmsTemplate transactedJmsTemplate()
	{
		val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
		pooledConnectionFactory.setMaxConnections(maxConnections);
		val jmsTemplate = new JmsTemplate(pooledConnectionFactory);
		jmsTemplate.setSessionTransacted(true);
		jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return jmsTemplate;
	}
}
