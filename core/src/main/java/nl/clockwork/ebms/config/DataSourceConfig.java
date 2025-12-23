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
package nl.clockwork.ebms.config;

import java.util.Arrays;
import java.util.Optional;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.transactionmanager.DefaultTransactionManagerType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
	public static final String BASEPATH = "classpath:/nl/clockwork/ebms/db/migration/";

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	public enum Location
	{
		DB2("jdbc:db2:", BASEPATH + "db2", false),
		DB2_STRICT("jdbc:db2:", BASEPATH + "db2.strict", true),
		H2("jdbc:h2:", BASEPATH + "h2", false),
		H2_STRICT("jdbc:h2:", BASEPATH + "h2.strict", true),
		HSQLDB("jdbc:hsqldb:", BASEPATH + "hsqldb", false),
		HSQLDB_STRICT("jdbc:hsqldb:", BASEPATH + "hsqldb.strict", true),
		MARIADB("jdbc:mariadb:", BASEPATH + "mariadb", false),
		MSSQL("jdbc:sqlserver:", BASEPATH + "mssql", false),
		ORACLE("jdbc:oracle:", BASEPATH + "oracle", false),
		ORACLE_STRICT("jdbc:oracle:", BASEPATH + "oracle.strict", true),
		POSTGRES("jdbc:postgresql:", BASEPATH + "postgresql", false),
		POSTGRES_STRICT("jdbc:postgresql:", BASEPATH + "postgresql.strict", true);

		String jdbcUrl;
		String location;
		boolean strict;

		public static Optional<String> getLocation(String jdbcUrl, boolean strict)
		{
			return Arrays.stream(values()).filter(l -> jdbcUrl.startsWith(l.jdbcUrl) && (l.strict == strict)).map(l -> l.location).findFirst();
		}
	}

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
	@Value("${ebms.jdbc.update}")
	boolean updateDb;
	@Value("${ebms.jdbc.strict}")
	boolean updateDbStrict;

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

	@EventListener(ContextRefreshedEvent.class)
	public void init()
	{
		if (updateDb)
		{
			val locations = Location.getLocation(jdbcUrl, updateDbStrict);
			locations.ifPresent(l ->
			{
				val config = Flyway.configure().dataSource(jdbcUrl, username, password).locations(l).ignoreMigrationPatterns("*:missing").outOfOrder(true);
				config.load().migrate();
			});
		}
	}
}
