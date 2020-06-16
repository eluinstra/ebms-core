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
package nl.clockwork.ebms.jms;

import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JMSConfig
{
	@Value("${jms.broker.start}")
	boolean jmsBrokerStart;
	@Value("${jms.broker.config}")
	String jmsBrokerConfig;
	@Value("${jms.brokerURL}")
	String jmsBrokerUrl;
	@Value("${jms.pool.maxPoolSize}")
	int maxPoolSize;
	@Autowired
	@Qualifier("jtaTransactionManager")
	PlatformTransactionManager jtaTransactionManager;

	@Bean("brokerFactory")
	public void brokerFactory() throws Exception
	{
		EbMSBrokerFactoryBean.init(jmsBrokerStart,jmsBrokerConfig);
	}

	@Bean
	public JmsTemplate jmsTemplate()
	{
		return new JmsTemplate(connectionFactory());
	}

	@Bean
	@DependsOn("brokerFactory")
	public ConnectionFactory connectionFactory()
	{
//		val result = new PooledConnectionFactory(jmsBrokerUrl);
//		result.setMaxConnections(maxPoolSize);
//		return result;
		val result = new PoolingConnectionFactory();
		result.setUniqueName("EbMSJMSConnectopnPool");
		result.setClassName("org.apache.activemq.ActiveMQXAConnectionFactory");
		result.setAllowLocalTransactions(true);
		result.setMaxPoolSize(maxPoolSize);
		result.setDriverProperties(createDriverProperties());
		result.init();
		return result;
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
//		result.put("userName",value);
//		result.put("password",value);
		result.put("brokerURL",jmsBrokerUrl);
		return result ;
	}

	@Bean("jmsTransactionManager")
	public PlatformTransactionManager jmsTransactionManager()
	{
		//TODO: JTA return new JmsTransactionManager(connectionFactory());
		return jtaTransactionManager;
	}

}
