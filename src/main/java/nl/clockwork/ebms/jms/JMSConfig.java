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
import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.atomikos.jms.AtomikosConnectionFactoryBean;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JMSConfig
{
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
	@Value("${jms.broker.start}")
	boolean jmsBrokerStart;
	@Value("${jms.broker.config}")
	String jmsBrokerConfig;
	@Value("${jms.brokerURL}")
	String jmsBrokerUrl;
	@Value("${jms.broker.username}")
	String username;
	@Value("${jms.broker.password}")
	String password;
	@Value("${jms.pool.minPoolSize}")
	int minPoolSize;
	@Value("${jms.pool.maxPoolSize}")
	int maxPoolSize;
	@Autowired
	@Qualifier("jtaTransactionManager")
	PlatformTransactionManager jtaTransactionManager;

	@Bean(name = "brokerFactory", destroyMethod = "destroy")
	public EbMSBrokerFactoryBean brokerFactory() throws Exception
	{
		return new EbMSBrokerFactoryBean(jmsBrokerStart,jmsBrokerUrl);
	}

	@Bean
	public JmsTemplate jmsTemplate() throws JMSException
	{
		return new JmsTemplate(connectionFactory());
	}

	@Bean(destroyMethod = "close")
	@DependsOn("brokerFactory")
	public ConnectionFactory connectionFactory() throws JMSException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val bitronixCF = new PoolingConnectionFactory();
				bitronixCF.setUniqueName(UUID.randomUUID().toString());
				bitronixCF.setClassName("org.apache.activemq.ActiveMQXAConnectionFactory");
				bitronixCF.setAllowLocalTransactions(true);
				bitronixCF.setMinPoolSize(minPoolSize);
				bitronixCF.setMaxPoolSize(maxPoolSize);
				bitronixCF.setDriverProperties(createDriverProperties());
				bitronixCF.init();
				return bitronixCF;
			case ATOMIKOS:
				val atomikosCF = new AtomikosConnectionFactoryBean();
				atomikosCF.setUniqueResourceName(UUID.randomUUID().toString());
				atomikosCF.setXaConnectionFactoryClassName("org.apache.activemq.ActiveMQXAConnectionFactory");
				atomikosCF.setLocalTransactionMode(true);
				atomikosCF.setMaxPoolSize(maxPoolSize);
				atomikosCF.setXaProperties(createDriverProperties());
				atomikosCF.init();
				return atomikosCF;
			default:
				val defaultCF = new CloseablePooledConnectionFactory(jmsBrokerUrl);
				defaultCF.setMaxConnections(maxPoolSize);
				return defaultCF;
		}
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
		result.put("brokerURL",jmsBrokerUrl);
		result.put("userName",username);
		result.put("password",password);
		return result ;
	}

	@Bean("jmsTransactionManager")
	public PlatformTransactionManager jmsTransactionManager() throws JMSException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
			case ATOMIKOS:
				return jtaTransactionManager;
			default:
				return new JmsTransactionManager(connectionFactory());
		}
	}
}
