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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
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
	@Value("${ebms.pool.idleTimeout}")
	int idleTimeout;
	@Value("${ebms.pool.maxLifetime}")
	int maxLifetime;
	@Value("${ebms.pool.testQuery}")
	String testQuery;
	@Value("${ebms.pool.minPoolSize}")
	int minPoolSize;
	@Value("${ebms.pool.maxPoolSize}")
	int maxPoolSize;
	
	@Bean(destroyMethod = "close")
	public DataSource dataSource() throws PropertyVetoException
	{
		//TODO: JTA
//		val config = new HikariConfig();
//		config.setDriverClassName(driverClassName);
//		config.setJdbcUrl(jdbcUrl);
//		config.setUsername(username);
//		config.setPassword(password);
//		config.setAutoCommit(isAutoCommit);
//		config.setConnectionTimeout(connectionTimeout);
//		config.setIdleTimeout(idleTimeout);
//		config.setMaxLifetime(maxLifetime);
//		config.setConnectionTestQuery(testQuery);
//		config.setMinimumIdle(minPoolSize);
//		config.setMaximumPoolSize(maxPoolSize);
//		return new HikariDataSource(config);
		val result = new PoolingDataSource();
		result.setUniqueName("EbMS");
		result.setClassName(driverClassName);
    //result.setLocalAutoCommit(isAutoCommit);
    result.setAllowLocalTransactions(true);
    result.setDriverProperties(createDriverProperties());
    result.setMaxIdleTime(idleTimeout);
    result.setMinPoolSize(minPoolSize);
    result.setMaxPoolSize(maxPoolSize);
//    result.setEnableJdbc4ConnectionTest(StringUtils.isEmpty(testQuery));
//    result.setTestQuery(testQuery);
    result.init();
    return result;
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
