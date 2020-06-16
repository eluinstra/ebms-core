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
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.internal.AtomikosSQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOConfig.TransactionManagerType;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
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
	public DataSource dataSource() throws PropertyVetoException, AtomikosSQLException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val bitronixDS = new PoolingDataSource();
				bitronixDS.setUniqueName("EbMSDataSource");
				bitronixDS.setClassName(driverClassName);
		    //result.setLocalAutoCommit(isAutoCommit);
		    bitronixDS.setAllowLocalTransactions(true);
		    bitronixDS.setDriverProperties(createDriverProperties());
		    bitronixDS.setMaxIdleTime(maxIdleTime);
		    bitronixDS.setMinPoolSize(minPoolSize);
		    bitronixDS.setMaxPoolSize(maxPoolSize);
		//    result.setEnableJdbc4ConnectionTest(StringUtils.isEmpty(testQuery));
		//    result.setTestQuery(testQuery);
		    bitronixDS.init();
		    return bitronixDS;
			case ATOMIKOS:
				val atomikosDS = new AtomikosDataSourceBean();
				atomikosDS.setUniqueResourceName("EbMSDataSource");
				atomikosDS.setXaDataSourceClassName(driverClassName);
				atomikosDS.setXaProperties(createDriverProperties());
				atomikosDS.setLocalTransactionMode(true);
				atomikosDS.setMaxIdleTime(maxIdleTime);
				atomikosDS.setMaxLifetime(maxLifetime);
				atomikosDS.setMinPoolSize(minPoolSize);
				atomikosDS.setMaxPoolSize(maxPoolSize);
				atomikosDS.setTestQuery(testQuery);
				atomikosDS.init();
				return atomikosDS;
			default:
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
	}

	private Properties createDriverProperties()
	{
		Properties result = new Properties();
    result.put("url",jdbcUrl);
    result.put("user",username);
    result.put("password",password);
		return result;
	}
}
