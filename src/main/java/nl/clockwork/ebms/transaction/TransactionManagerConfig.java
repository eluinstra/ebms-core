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
package nl.clockwork.ebms.transaction;

import java.util.UUID;

import javax.transaction.SystemException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
	@Value("${transactionManager.transactionTimeout}")
	int transactionTimeout;

	public enum TransactionManagerType
	{
		DEFAULT, BITRONIX, ATOMIKOS;
	}

	@Bean("jtaTransactionManager")
	@DependsOn("btmConfig")
	public PlatformTransactionManager jtaTransactionManager() throws SystemException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val transactionManager = TransactionManagerServices.getTransactionManager();
				return new JtaTransactionManager(transactionManager,transactionManager);
			case ATOMIKOS:
				val userTransactionManager = new UserTransactionManager();
				userTransactionManager.setTransactionTimeout(transactionTimeout);
				userTransactionManager.setForceShutdown(true);
				return new JtaTransactionManager(userTransactionManager,userTransactionManager);
			default:
				return new DummyTransactionManager();
		}
	}

	@Bean("btmConfig")
	public void btmConfig()
	{
		bitronix.tm.Configuration config = TransactionManagerServices.getConfiguration();
		config.setServerId(UUID.randomUUID().toString());
		config.setDefaultTransactionTimeout(transactionTimeout);
	}

	public static TransactionDefinition createTransactionDefinition()
	{
		val result = new DefaultTransactionDefinition();
		result.setName(UUID.randomUUID().toString());
		result.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		return result;
	}
}
