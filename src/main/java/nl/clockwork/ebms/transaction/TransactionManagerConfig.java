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


import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import javax.sql.DataSource;
import javax.transaction.SystemException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

@Configuration
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionManagerConfig
{
	public enum TransactionManagerType
	{
		DEFAULT, ATOMIKOS;
	}

	@Value("${transactionManager.transactionTimeout}")
	int transactionTimeout;

	@Bean("dataSourceTransactionManager")
	@Conditional(DefaultTransactionManagerType.class)
	public PlatformTransactionManager dataSourceTransactionManager(DataSource dataSource)
	{
		return new DataSourceTransactionManager(dataSource);
	}

	@Bean(name = {"dataSourceTransactionManager", "jmsTransactionManager"})
	@Conditional(AtomikosTransactionManagerType.class)
	public JtaTransactionManager atomikosJtaTransactionManager() throws SystemException
	{
		return new JtaTransactionManager(createUserTransaction(), createTransactionManager());
	}

	private UserTransactionImp createUserTransaction() throws SystemException
	{
		val result = new UserTransactionImp();
		result.setTransactionTimeout(transactionTimeout);
		return result;
	}

	private UserTransactionManager createTransactionManager() throws SystemException
	{
		val result = new UserTransactionManager();
		result.setTransactionTimeout(transactionTimeout);
		result.setForceShutdown(false);
		return result;
	}

	public static class DefaultTransactionManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("transactionManager.type", TransactionManagerType.class, TransactionManagerType.DEFAULT)
					== TransactionManagerType.DEFAULT;
		}
	}

	public static class AtomikosTransactionManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("transactionManager.type", TransactionManagerType.class, TransactionManagerType.DEFAULT)
					== TransactionManagerType.ATOMIKOS;
		}
	}
}
