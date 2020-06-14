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
package nl.clockwork.ebms.dao;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
	@Value("${ebms.jdbc.driverClassName}")
	String driverClass;
	@Value("${ebms.jdbc.url}")
	String jdbcUrl;
	@Value("${ebms.jdbc.username}")
	String user;
	@Value("${ebms.jdbc.password}")
	String password;
	@Value("${ebms.pool.acquireIncrement}")
	int acquireIncrement;
	@Value("${ebms.pool.initialPoolSize}")
	int initialPoolSize;
	@Value("${ebms.pool.maxPoolSize}")
	int maxPoolSize;
	@Value("${ebms.pool.minPoolSize}")
	int minPoolSize;
	@Value("${ebms.pool.maxConnectionAge}")
	int maxConnectionAge;
	@Value("${ebms.pool.maxIdleTime}")
	int maxIdleTime;
	@Value("${ebms.pool.maxIdleTimeExcessConnections}")
	int maxIdleTimeExcessConnections;
	@Value("${ebms.pool.idleConnectionTestPeriod}")
	int idleConnectionTestPeriod;
	@Value("${ebms.pool.preferredTestQuery}")
	String preferredTestQuery;
	@Value("${ebms.pool.testConnectionOnCheckin}")
	boolean testConnectionOnCheckin;
	@Value("${ebms.pool.testConnectionOnCheckout}")
	boolean testConnectionOnCheckout;
	@Value("${ebms.pool.maxStatements}")
	int maxStatements;
	@Value("${ebms.pool.maxStatementsPerConnection}")
	int maxStatementsPerConnection;
	@Value("${ebms.pool.acquireRetryAttempts}")
	int acquireRetryAttempts;
	@Value("${ebms.pool.acquireRetryDelay}")
	int acquireRetryDelay;
	@Value("${ebms.pool.breakAfterAcquireFailure}")
	boolean breakAfterAcquireFailure;
	@Value("${ebms.pool.autoCommitOnClose}")
	boolean autoCommitOnClose;
	@Value("${ebms.pool.debugUnreturnedConnectionStackTraces}")
	boolean debugUnreturnedConnectionStackTraces;
	@Value("${ebms.pool.unreturnedConnectionTimeout}")
	int unreturnedConnectionTimeout;
	@Value("${ebms.pool.checkoutTimeout}")
	int checkoutTimeout;
	@Value("${ebms.pool.maxAdministrativeTaskTime}")
	int maxAdministrativeTaskTime;
	@Value("${ebms.pool.numHelperThreads}")
	int numHelperThreads;
	
	@Bean
	public DataSource dataSource() throws PropertyVetoException
	{
		val result = new ComboPooledDataSource();
		result.setDriverClass(driverClass);
		result.setJdbcUrl(jdbcUrl);
		result.setUser(user);
		result.setPassword(password);
		result.setAcquireIncrement(acquireIncrement);
		result.setInitialPoolSize(initialPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		result.setMinPoolSize(minPoolSize);
		result.setMaxConnectionAge(maxConnectionAge);
		result.setMaxIdleTime(maxIdleTime);
		result.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
		result.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
		result.setPreferredTestQuery(preferredTestQuery);
		result.setTestConnectionOnCheckin(testConnectionOnCheckin);
		result.setTestConnectionOnCheckout(testConnectionOnCheckout);
		result.setMaxStatements(maxStatements);
		result.setMaxStatementsPerConnection(maxStatementsPerConnection);
		result.setAcquireRetryAttempts(acquireRetryAttempts);
		result.setAcquireRetryDelay(acquireRetryDelay);
		result.setBreakAfterAcquireFailure(breakAfterAcquireFailure);
		result.setAutoCommitOnClose(autoCommitOnClose);
		result.setDebugUnreturnedConnectionStackTraces(debugUnreturnedConnectionStackTraces);
		result.setUnreturnedConnectionTimeout(unreturnedConnectionTimeout);
		result.setCheckoutTimeout(checkoutTimeout);
		result.setMaxAdministrativeTaskTime(maxAdministrativeTaskTime);
		result.setNumHelperThreads(numHelperThreads);
		return result;
	}
}
