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

import java.beans.PropertyVetoException;
import java.sql.Types;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import javax.transaction.SystemException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.internal.AtomikosSQLException;
import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.HSQLDBTemplates;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLServerTemplates;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.querydsl.CachedOutputStreamType;
import nl.clockwork.ebms.querydsl.CollaborationProtocolAgreementType;
import nl.clockwork.ebms.querydsl.EbMSEventStatusType;
import nl.clockwork.ebms.querydsl.EbMSMessageEventTypeType;
import nl.clockwork.ebms.querydsl.EbMSMessageStatusType;
import nl.clockwork.ebms.querydsl.InstantType;
import nl.clockwork.ebms.querydsl.X509CertificateType;
import nl.clockwork.ebms.transaction.TransactionManagerConfig.TransactionManagerType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataSourceConfig
{
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
	@Autowired
	@Qualifier("jtaTransactionManager")
	PlatformTransactionManager jtaTransactionManager;
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
	
	@Bean
	public SQLQueryFactory queryFactory() throws AtomikosSQLException, PropertyVetoException
	{
		val provider = new SpringConnectionProvider(dataSource());
		return new SQLQueryFactory(querydslConfiguration(),provider);
	}

	@Bean
	public com.querydsl.sql.Configuration querydslConfiguration() throws AtomikosSQLException, PropertyVetoException
	{
		val templates = getSQLTemplates();
		val configuration = new com.querydsl.sql.Configuration(templates);
		configuration.setExceptionTranslator(new SpringExceptionTranslator());
		configuration.register("CPA","CPA",new CollaborationProtocolAgreementType(Types.CLOB));
		configuration.register("CERTIFICATE_MAPPING","SOURCE",new X509CertificateType(Types.BLOB));
		configuration.register("CERTIFICATE_MAPPING","DESTINATION",new X509CertificateType(Types.BLOB));
		configuration.register("EBMS_MESSAGE_EVENT","EVENT_TYPE",new EbMSMessageEventTypeType(Types.SMALLINT));
		configuration.register("EBMS_EVENT","TIME_TO_LIVE",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_EVENT","TIME_STAMP",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_EVENT_LOG","TIME_STAMP",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_EVENT_LOG","STATUS",new EbMSEventStatusType(Types.SMALLINT));
		configuration.register("EBMS_MESSAGE","TIME_STAMP",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_MESSAGE","TIME_TO_LIVE",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_MESSAGE","PERSIST_TIME",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_MESSAGE","STATUS",new EbMSMessageStatusType(Types.SMALLINT));
		configuration.register("EBMS_MESSAGE","STATUS_TIME",new InstantType(Types.TIMESTAMP));
		configuration.register("EBMS_ATTACHMENT","CONTENT",new CachedOutputStreamType(Types.BLOB));
		return configuration;
	}

	@Bean("dataSourceTransactionManager")
	public PlatformTransactionManager dataSourceTransactionManager() throws SystemException, AtomikosSQLException, PropertyVetoException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
			case ATOMIKOS:
				return jtaTransactionManager;
			default:
				return new DataSourceTransactionManager(dataSource());
		}
	}

	@Bean(destroyMethod = "close")
	public DataSource dataSource() throws PropertyVetoException, AtomikosSQLException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val bitronixDS = new PoolingDataSource();
				bitronixDS.setUniqueName(UUID.randomUUID().toString());
				bitronixDS.setClassName(driverClassName);
		    //result.setLocalAutoCommit(isAutoCommit);
		    bitronixDS.setAllowLocalTransactions(true);
		    bitronixDS.setDriverProperties(createDriverProperties());
		    bitronixDS.setMaxIdleTime(maxIdleTime);
		    bitronixDS.setMinPoolSize(minPoolSize);
		    bitronixDS.setMaxPoolSize(maxPoolSize);
		    bitronixDS.setEnableJdbc4ConnectionTest(StringUtils.isEmpty(testQuery));
		    bitronixDS.setTestQuery(testQuery);
		    bitronixDS.init();
		    return bitronixDS;
			case ATOMIKOS:
				val atomikosDS = new AtomikosDataSourceBean();
				atomikosDS.setUniqueResourceName(UUID.randomUUID().toString());
				atomikosDS.setXaDataSourceClassName(driverClassName);
				atomikosDS.setXaProperties(createDriverProperties());
				atomikosDS.setLocalTransactionMode(true);
				atomikosDS.setMaxIdleTime(maxIdleTime);
				atomikosDS.setMaxLifetime(maxLifetime);
				atomikosDS.setMinPoolSize(minPoolSize);
				atomikosDS.setMaxPoolSize(maxPoolSize);
				if (StringUtils.isNotEmpty(testQuery))
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

	private SQLTemplates getSQLTemplates() throws AtomikosSQLException, PropertyVetoException
	{
		switch(transactionManagerType)
		{
			case BITRONIX:
			case ATOMIKOS:
				return createXASQLTemplates();
			default:
				return createDefaultSQLTemplates();
		}
	}

	private SQLTemplates createXASQLTemplates() throws AtomikosSQLException, PropertyVetoException
	{
		val dataSource = dataSource();
		val driverClassName = dataSource  instanceof PoolingDataSource ? ((PoolingDataSource)dataSource).getClassName() : ((AtomikosDataSourceBean)dataSource).getXaDataSourceClassName();
		switch (driverClassName)
		{
			case "org.hsqldb.jdbc.pool.JDBCXADataSource":
				return HSQLDBTemplates.builder().build();
			case "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource":
			case "org.mariadb.jdbc.MySQLDataSource":
				return MySQLTemplates.builder().build();
			case "org.postgresql.xa.PGXADataSource":
				return PostgreSQLTemplates.builder().build();
			case "oracle.jdbc.xa.client.OracleXADataSource":
				return OracleTemplates.builder().build();
			case "com.microsoft.sqlserver.jdbc.SQLServerXAResource":
			case "com.microsoft.sqlserver.jdbc.SQLServerXADataSource":
				return SQLServerTemplates.builder().build();
			case "com.ibm.db2.jcc.DB2XADataSource":
				return DB2Templates.builder().build();
			default:
				throw new RuntimeException("SQL Driver " + driverClassName + " not recognized!");
		}
	}

	private SQLTemplates createDefaultSQLTemplates() throws AtomikosSQLException, PropertyVetoException
	{
		switch (((HikariDataSource)dataSource()).getDriverClassName())
		{
			case "org.hsqldb.jdbcDriver":
				return HSQLDBTemplates.builder().build();
			case "com.mysql.jdbc.Driver":
			case "org.mariadb.jdbc.Driver":
				return MySQLTemplates.builder().build();
			case "org.postgresql.Driver":
				return PostgreSQLTemplates.builder().build();
			case "oracle.jdbc.OracleDriver":
				return OracleTemplates.builder().build();
			case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
				return SQLServerTemplates.builder().build();
			case "com.ibm.db2.jcc.DB2Driver":
				return DB2Templates.builder().build();
			default:
				throw new RuntimeException("SQL Driver " + ((HikariDataSource)dataSource()).getDriverClassName() + " not recognized!");
		}
	}

	private Properties createDriverProperties()
	{
		val result = new Properties();
    result.put("url",jdbcUrl);
    result.put("user",username);
    result.put("password",password);
		return result;
	}
}
