/*
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


import com.atomikos.jms.AtomikosConnectionFactoryBean;
import java.util.UUID;
import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.AtomikosTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.DefaultTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

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

	@Bean(name = "brokerFactory", destroyMethod = "destroy")
	public EbMSBrokerFactoryBean brokerFactory() throws Exception
	{
		return new EbMSBrokerFactoryBean(jmsBrokerStart, jmsBrokerConfig);
	}

	@Bean
	@Conditional(DefaultTransactionManagerType.class)
	@DependsOn("brokerFactory")
	public ConnectionFactory pooledConnectionFactor()
	{
		val result = new PooledConnectionFactory(jmsBrokerUrl);
		result.setMaxConnections(maxPoolSize);
		return result;
	}

	@Bean("jmsTransactionManager")
	@Conditional(DefaultTransactionManagerType.class)
	public PlatformTransactionManager jmsTransactionManager(ConnectionFactory connectionFactory)
	{
		return new JmsTransactionManager(connectionFactory);
	}

	@Bean(initMethod = "init", destroyMethod = "close")
	@Conditional(AtomikosTransactionManagerType.class)
	@DependsOn("brokerFactory")
	public ConnectionFactory atomikosConnectionFactoryBean()
	{
		val result = new AtomikosConnectionFactoryBean();
		result.setUniqueResourceName(UUID.randomUUID().toString());
		result.setXaConnectionFactory(createXAConnectionFactory());
		result.setLocalTransactionMode(false);
		result.setIgnoreSessionTransactedFlag(true);
		result.setMinPoolSize(minPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		result.setBorrowConnectionTimeout(30);
		result.setIgnoreSessionTransactedFlag(true);
		result.setMaintenanceInterval(60);
		result.setMaxIdleTime(60);
		result.setMaxLifetime(0);
		result.setReapTimeout(0);
		return result;
	}

	private XAConnectionFactory createXAConnectionFactory()
	{
		val result = new ActiveMQXAConnectionFactory(jmsBrokerUrl);
		if (StringUtils.isNotEmpty(username))
		{
			result.setUserName(username);
			result.setPassword(password);
		}
		return result;
	}
}
