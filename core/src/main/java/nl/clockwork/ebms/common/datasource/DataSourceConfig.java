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
package nl.clockwork.ebms.common.datasource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
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
	@Value("${ebms.jdbc.update}")
	boolean updateDb;
	@Value("${ebms.jdbc.strict}")
	boolean updateDbStrict;
	@Value("${ebms.jdbc.migrationBasePath:classpath:/db/migration/}")
	String migrationBasePath;
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
	@DependsOn("databaseServer")
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

	@Bean(name = "databaseServer")
	@Conditional(NotStartDatabaseServerType.class)
	public Object startH2DBServer()
	{
		return new Object();
	}

	@EventListener(ContextRefreshedEvent.class)
	public void init()
	{
		if (updateDb)
		{
			val location = migrationBasePath + (updateDbStrict ? "strict/" : "default/");
			val config = Flyway.configure().dataSource(jdbcUrl, username, password).locations(location).ignoreMigrationPatterns("*:missing").outOfOrder(true);
			config.load().migrate();
		}
	}

	public static class NotStartDatabaseServerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return !context.getEnvironment().getProperty("database.start", Boolean.class, false);
		}
	}
}
