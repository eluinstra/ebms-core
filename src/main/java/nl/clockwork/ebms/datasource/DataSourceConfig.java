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
package nl.clockwork.ebms.datasource;


import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.ibm.db2.jcc.DB2XADataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.IsolationLevel;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.AtomikosTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.DefaultTransactionManagerType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

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
		DB2("jdbc:db2:",BASEPATH + "db2",false),
		DB2_STRICT("jdbc:db2:",BASEPATH + "db2.strict",true),
		H2("jdbc:h2:",BASEPATH + "h2",false),
		H2_STRICT("jdbc:h2:",BASEPATH + "h2.strict",true),
		HSQLDB("jdbc:hsqldb:",BASEPATH + "hsqldb",false),
		HSQLDB_STRICT("jdbc:hsqldb:",BASEPATH + "hsqldb.strict",true),
		MARIADB("jdbc:mariadb:",BASEPATH + "mariadb",false),
		MSSQL("jdbc:sqlserver:",BASEPATH + "mssql",false),
		ORACLE("jdbc:oracle:",BASEPATH + "oracle",false),
		ORACLE_STRICT("jdbc:oracle:",BASEPATH + "oracle.strict",true),
		POSTGRES("jdbc:postgresql:",BASEPATH + "postgresql",false),
		POSTGRES_STRICT("jdbc:postgresql:",BASEPATH + "postgresql.strict",true);

		String jdbcUrl;
		String location;
		boolean strict;

		public static Optional<String> getLocation(String jdbcUrl, boolean strict)
		{
			return Arrays.stream(values()).filter(l -> jdbcUrl.startsWith(l.jdbcUrl) && (l.strict == strict)).map(l -> l.location).findFirst();
		}
	}

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
	@Value("${ebms.jdbc.update}")
	boolean updateDb;
	@Value("${ebms.jdbc.strict}")
	boolean updateDbStrict;
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
	@Conditional(AtomikosTransactionManagerType.class)
	public DataSource atomikosDataSourceBean() throws SQLException
	{
		val result = new AtomikosDataSourceBean();
		result.setUniqueResourceName(UUID.randomUUID().toString());
		if (jdbcUrl.contains("db2"))
			createDB2XADataSource().ifPresentOrElse(result::setXaDataSource,() ->
			{
				throw new IllegalStateException("Error creating DB2XADataSource");
			});
		else
		{
			result.setXaDataSourceClassName(driverClassName);
			result.setXaProperties(createDriverProperties());
		}
		if (isolationLevel != null)
			result.setDefaultIsolationLevel(isolationLevel.getLevelId());
		result.setLocalTransactionMode(true);
		result.setMaxIdleTime(maxIdleTime);
		result.setMaxLifetime(maxLifetime);
		result.setMinPoolSize(minPoolSize);
		result.setMaxPoolSize(maxPoolSize);
		if (StringUtils.isNotEmpty(testQuery))
			result.setTestQuery(testQuery);
		result.setBorrowConnectionTimeout(30);
		result.setConcurrentConnectionValidation(true);
		result.setLoginTimeout(0);
		result.setMaintenanceInterval(60);
		result.setMaxIdleTime(60);
		result.setMaxLifetime(0);
		result.setReapTimeout(0);
		result.init();
		return result;
	}

	private Optional<XADataSource> createDB2XADataSource()
	{
		return matchJdbcUrl(jdbcUrl).map(matcher ->
		{
			val result = new DB2XADataSource();
			result.setDatabaseName(matcher.group(3));
			result.setUser(username);
			result.setPassword(password);
			result.setServerName(matcher.group(1));
			result.setPortNumber(Integer.parseInt(matcher.group(2)));
			result.setDriverType(4);
			return result;
		});
	}

	private Optional<Matcher> matchJdbcUrl(String jdbcUrl)
	{
		val p = Pattern.compile("^jdbc:db2://([^:]+):(\\d+)/(.*)$");
		val m = p.matcher(jdbcUrl);
		return m.find() ? Optional.of(m) : Optional.empty();
	}

	@Bean
	public void flyway()
	{
		if (updateDb)
		{
			val locations = Location.getLocation(jdbcUrl,updateDbStrict);
			locations.ifPresent(l ->
			{
				val config = Flyway.configure().dataSource(jdbcUrl,username,password).locations(l).ignoreMigrationPatterns("*:missing").outOfOrder(true);
				config.load().migrate();
			});
		}
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
		if (driverClassName.contains("sqlserver"))
			result.put("URL",jdbcUrl);
		else if (driverClassName.contains("oracle") && driverClassName.contains("xa"))
			result.put("URL",jdbcUrl);
		else
			result.put("url",jdbcUrl);
		result.put("user",username);
		result.put("password",password);
		return result;
	}
}
