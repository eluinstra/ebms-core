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
package nl.clockwork.ebms.datasource;

import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.internal.AtomikosSQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.IsolationLevel;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.AtomikosTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.BitronixTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.DefaultTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
	@Value("${transactionManager.isolationLevel}")
	IsolationLevel isolationLevel;
	@Value("${ebms.jdbc.driverClassName}")
	String driverClassName;
	@Value("${ebms.jdbc.url}")
	String jdbcUrl;
	@Value("${ebms.jdbc.username}")
	String username;
	@Value("${ebms.jdbc.password}")
	String password;
	@Value("${ebms.pool.autoCommit}")
	boolean isAutoCommit;
	@Value("${ebms.pool.connectionTimeout}")
	int connectionTimeout;
	@Value("${ebms.pool.maxIdleTime}")
	int maxIdleTime;
	@Value("${ebms.pool.maxLifetime}")
	int maxLifetime;
	@Value("${ebms.pool.testQuery}")
	String testQuery;
	@Value("${ebms.pool.minPoolSize}")
	int minPoolSize;
	@Value("${ebms.pool.maxPoolSize}")
	int maxPoolSize;
	
	@Bean(destroyMethod = "close")
	@Conditional(DefaultTransactionManagerType.class)
	public DataSource hikariDataSource()
	{
		val config = new HikariConfig();
		config.setDriverClassName(driverClassName);
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(username);
		config.setPassword(password);
		config.setAutoCommit(isAutoCommit);
		config.setConnectionTimeout(connectionTimeout);
		config.setIdleTimeout(maxIdleTime);
		config.setMaxLifetime(maxLifetime);
		config.setConnectionTestQuery(testQuery);
		config.setMinimumIdle(minPoolSize);
		config.setMaximumPoolSize(maxPoolSize);
		return new HikariDataSource(config);
	}

	@Bean(destroyMethod = "close")
	@Conditional(BitronixTransactionManagerType.class)
	public DataSource poolingDataSource()
	{
		val result = new PoolingDataSource();
		result.setUniqueName(UUID.randomUUID().toString());
		result.setClassName(driverClassName);
		if (isolationLevel != null)
			result.setIsolationLevel(isolationLevel.name());
    result.setAllowLocalTransactions(false);
    result.setDriverProperties(createDriverProperties());
    result.setMaxIdleTime(maxIdleTime);
    result.setMinPoolSize(minPoolSize);
    result.setMaxPoolSize(maxPoolSize);
    result.setEnableJdbc4ConnectionTest(StringUtils.isEmpty(testQuery));
    result.setTestQuery(testQuery);
    result.init();
    return result;
	}

	@Bean(destroyMethod = "close")
	@Conditional(AtomikosTransactionManagerType.class)
	public DataSource atomikosDataSourceBean() throws AtomikosSQLException
	{
		val result = new AtomikosDataSourceBean();
		result.setUniqueResourceName(UUID.randomUUID().toString());
		result.setXaDataSourceClassName(driverClassName);
		result.setXaProperties(createDriverProperties());
		if (isolationLevel != null)
			result.setDefaultIsolationLevel(isolationLevel.getLevelId());
		result.setLocalTransactionMode(false);
		result.setMaxIdleTime(maxIdleTime);
		result.setMaxLifetime(maxLifetime);
		result.setMinPoolSize(minPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		if (StringUtils.isNotEmpty(testQuery))
			result.setTestQuery(testQuery);
		result.init();
		return result;
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
		if (driverClassName.contains("sqlserver"))
			result.put("URL",jdbcUrl);
		else
			result.put("url",jdbcUrl);
    result.put("user",username);
    result.put("password",password);
		return result;
	}
}
