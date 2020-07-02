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
import javax.jms.XAConnectionFactory;

import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.atomikos.jms.AtomikosConnectionFactoryBean;

import bitronix.tm.resource.jms.PoolingConnectionFactory;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.AtomikosTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.BitronixTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.DefaultTransactionManagerType;
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

	@Bean(name = "brokerFactory", destroyMethod = "destroy")
	public EbMSBrokerFactoryBean brokerFactory() throws Exception
	{
		return new EbMSBrokerFactoryBean(jmsBrokerStart,jmsBrokerConfig);
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

	@Bean(initMethod = "init", destroyMethod = "close")
	@Conditional(BitronixTransactionManagerType.class)
	@DependsOn("brokerFactory")
	public ConnectionFactory poolingConnectionFactory()
	{
		val result = new PoolingConnectionFactory();
		result.setUniqueName(UUID.randomUUID().toString());
		result.setClassName("org.apache.activemq.ActiveMQXAConnectionFactory");
		result.setAllowLocalTransactions(false);
		result.setMinPoolSize(minPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		result.setDriverProperties(createDriverProperties());
		return result;
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
		return result;
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
		result.put("brokerURL",jmsBrokerUrl);
		if (StringUtils.isNotEmpty(username))
		{
			result.put("userName",username);
			result.put("password",password);
		}
		return result ;
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
