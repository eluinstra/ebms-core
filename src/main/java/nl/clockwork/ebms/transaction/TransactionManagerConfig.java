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
package nl.clockwork.ebms.transaction;

import java.util.UUID;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;
import javax.transaction.SystemException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import bitronix.tm.TransactionManagerServices;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionManagerConfig
{
	public enum TransactionManagerType
	{
		DEFAULT, BITRONIX, ATOMIKOS;
	}
	@Value("${transactionManager.transactionTimeout}")
	int transactionTimeout;
	@Autowired
	DataSource dataSource;
	@Autowired
	ConnectionFactory connectionFactory;

	@Bean("dataSourceTransactionManager")
	@Conditional(DefaultTransactionManagerType.class)
	public PlatformTransactionManager dataSourceTransactionManager()
	{
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean("jmsTransactionManager")
	@Conditional(DefaultTransactionManagerType.class)
	public PlatformTransactionManager jmsTransactionManager()
	{
		return new JmsTransactionManager(connectionFactory);
	}

	@Bean(name = {"dataSourceTransactionManager","jmsTransactionManager"})
	@Conditional(BitronixTransactionManagerType.class)
	@DependsOn("btmConfig")
	public PlatformTransactionManager bitronixJtaTransactionManager()
	{
		val transactionManager = TransactionManagerServices.getTransactionManager();
		return new JtaTransactionManager(transactionManager,transactionManager);
	}

	@Bean(name = {"dataSourceTransactionManager","jmsTransactionManager"})
	@Conditional(AtomikosTransactionManagerType.class)
	public JtaTransactionManager AtomikosJtaTransactionManager() throws SystemException
	{
		val transactionManager = new UserTransactionManager();
		transactionManager.setTransactionTimeout(transactionTimeout);
		transactionManager.setForceShutdown(false);
		val userTransaction = new UserTransactionImp();
		userTransaction.setTransactionTimeout(transactionTimeout);
		return new JtaTransactionManager(userTransaction,transactionManager);
	}

	@Conditional(BitronixTransactionManagerType.class)
	@Bean("btmConfig")
	public void btmConfig()
	{
		val config = TransactionManagerServices.getConfiguration();
		config.setServerId(UUID.randomUUID().toString());
		config.setDefaultTransactionTimeout(transactionTimeout);
	}

	public static class DefaultTransactionManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("transactionManager.type",TransactionManagerType.class,TransactionManagerType.DEFAULT) == TransactionManagerType.DEFAULT;
		}
	}
	public static class BitronixTransactionManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("transactionManager.type",TransactionManagerType.class,TransactionManagerType.DEFAULT) == TransactionManagerType.BITRONIX;
		}
	}
	public static class AtomikosTransactionManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("transactionManager.type",TransactionManagerType.class,TransactionManagerType.DEFAULT) == TransactionManagerType.ATOMIKOS;
		}
	}
}
