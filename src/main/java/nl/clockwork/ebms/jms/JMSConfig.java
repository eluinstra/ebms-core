package nl.clockwork.ebms.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JMSConfig
{
	@Value("${jms.broker.start}")
	boolean jmsBrokerStart;
	@Value("${jms.broker.config}")
	String jmsBrokerConfig;

	@Bean("brokerFactory")
	public void brokerFactory() throws Exception
	{
		EbMSBrokerFactoryBean.init(jmsBrokerStart,jmsBrokerConfig);
	}
}
