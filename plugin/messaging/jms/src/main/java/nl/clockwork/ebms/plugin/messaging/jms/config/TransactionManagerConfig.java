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
package nl.clockwork.ebms.plugin.messaging.jms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;

import jakarta.transaction.SystemException;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.transactionmanager.AtomikosTransactionManagerType;

@Configuration
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionManagerConfig
{
	@Value("${transactionManager.transactionTimeout}")
	int transactionTimeout;

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
}
