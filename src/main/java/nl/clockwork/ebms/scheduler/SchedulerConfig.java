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
package nl.clockwork.ebms.scheduler;

import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.experimental.FieldDefaults;

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
		DB2("jdbc:db2:","org.quartz.impl.jdbcjobstore.DB2v8Delegate"),
		H2("jdbc:h2:","org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		HSQLDB("jdbc:hsqldb:","org.quartz.impl.jdbcjobstore.HSQLDBDelegate"),
		MARIADB("jdbc:mariadb:","org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		MSSQL("jdbc:sqlserver:","org.quartz.impl.jdbcjobstore.MSSQLDelegate"),
		MYSQL("jdbc:mysql:","org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
		ORACLE("jdbc:oracle:","org.quartz.impl.jdbcjobstore.oracle.OracleDelegate"),
		POSTGRES("jdbc:postgresql:","org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
		
		String jdbcUrl;
		String driverDelegateClass;
		
		public static Optional<String> getClass(String jdbcUrl)
		{
			return Arrays.stream(values())
					.filter(l -> jdbcUrl.startsWith(l.jdbcUrl))
					.map(l -> l.driverDelegateClass)
					.findFirst();
		}
	}

	@Autowired
	@Qualifier("dataSourceTransactionManager")
	PlatformTransactionManager dataSourceTransactionManager;
	@Autowired
	DataSource dataSource;
	@Value("${ebms.jdbc.url}")
	String jdbcUrl;
	@Value("${deliveryTaskHandler.maxThreads}")
	String threadCount;
	@Value("${deliveryTaskHandler.quartz.driverDelegateClass}")
	String driverDelegateClass;
	@Value("${deliveryTaskHandler.quartz.isClustered}")
	String isClustered;

	@Bean
	public SchedulerFactoryBean scheduler(JobFactory jobFactory)
	{
		val result = new SchedulerFactoryBean();
    result.setQuartzProperties(quartzProperties());
    result.setJobFactory(jobFactory);
    result.setTransactionManager(dataSourceTransactionManager);
		result.setDataSource(dataSource);
		return result;
	}

	@Bean
	public JobFactory jobFactory(ApplicationContext applicationContext)
	{
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

  private Properties quartzProperties()
	{
		val result = new Properties();
		result.put("org.quartz.scheduler.instanceId","AUTO");
		result.put("org.quartz.threadPool.threadCount",threadCount);
		result.put("org.quartz.jobStore.class","org.quartz.impl.jdbcjobstore.JobStoreCMT");
		result.put("org.quartz.jobStore.driverDelegateClass",driverDelegateClass == null ? DriverDelegate.getClass(jdbcUrl) : driverDelegateClass);
		result.put("org.quartz.jobStore.isClustered",isClustered);
		return result;
	}
}
