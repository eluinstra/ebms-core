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
package nl.clockwork.ebms.scheduler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Arrays;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.delivery.task.DeliveryTaskHandlerConfig.DeliveryTaskHandlerType;
import nl.clockwork.ebms.delivery.task.DeliveryTaskHandlerConfig.QuartzTaskHandlerType;
import org.apache.commons.lang3.StringUtils;
import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SchedulerConfig
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	public enum DriverDelegate
	{
		DB2("jdbc:db2:", "org.quartz.impl.jdbcjobstore.DB2v8Delegate"),
		H2("jdbc:h2:", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		HSQLDB("jdbc:hsqldb:", "org.quartz.impl.jdbcjobstore.HSQLDBDelegate"),
		MARIADB("jdbc:mariadb:", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		MSSQL("jdbc:sqlserver:", "org.quartz.impl.jdbcjobstore.MSSQLDelegate"),
		MYSQL("jdbc:mysql:", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		ORACLE("jdbc:oracle:", "org.quartz.impl.jdbcjobstore.oracle.OracleDelegate"),
		POSTGRES("jdbc:postgresql:", "org.quartz.impl.jdbcjobstore.HSQLDBDelegate");

		String jdbcUrl;
		String driverDelegateClass;

		public static String getClass(String jdbcUrl)
		{
			return Arrays.stream(values()).filter(l -> jdbcUrl.startsWith(l.jdbcUrl)).map(l -> l.driverDelegateClass).findFirst().get();
		}
	}

	@Autowired
	@Qualifier("dataSourceTransactionManager")
	PlatformTransactionManager dataSourceTransactionManager;
	@Autowired
	DataSource dataSource;
	@Value("${deliveryTaskHandler.type}")
	DeliveryTaskHandlerType deliveryTaskHandlerType;
	@Value("${deliveryTaskHandler.quartz.jdbc.driverClassName}")
	String driverClassName;
	@Value("${deliveryTaskHandler.quartz.jdbc.selectWithLockSQL}")
	String selectWithLockSQL;
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
	@Value("${deliveryTaskHandler.maxThreads}")
	String threadCount;
	@Value("${deliveryTaskHandler.quartz.driverDelegateClass}")
	String driverDelegateClass;
	@Value("${deliveryTaskHandler.quartz.isClustered}")
	String isClustered;

	@Bean
	@Conditional(QuartzTaskHandlerType.class)
	public SchedulerFactoryBean scheduler(JobFactory jobFactory)
	{
		val result = new SchedulerFactoryBean();
		result.setQuartzProperties(quartzProperties());
		result.setJobFactory(jobFactory);
		result.setTransactionManager(dataSourceTransactionManager);
		result.setDataSource(dataSource);
		if (deliveryTaskHandlerType == DeliveryTaskHandlerType.QUARTZ_JMS)
			result.setNonTransactionalDataSource(hikariDataSource());
		return result;
	}

	@Bean
	@Conditional(QuartzTaskHandlerType.class)
	public JobFactory jobFactory(ApplicationContext applicationContext)
	{
		val result = new AutowiringSpringBeanJobFactory();
		result.setApplicationContext(applicationContext);
		return result;
	}

	private Properties quartzProperties()
	{
		val result = new Properties();
		result.put("org.quartz.scheduler.instanceId", "AUTO");
		result.put("org.quartz.threadPool.threadCount", threadCount);
		result.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreCMT");
		result.put("org.quartz.jobStore.driverDelegateClass", StringUtils.isEmpty(driverDelegateClass) ? DriverDelegate.getClass(jdbcUrl) : driverDelegateClass);
		if (StringUtils.isNotEmpty(selectWithLockSQL))
			result.put("org.quartz.jobStore.selectWithLockSQL", selectWithLockSQL);
		result.put("org.quartz.jobStore.isClustered", isClustered);
		return result;
	}

	private DataSource hikariDataSource()
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
}
