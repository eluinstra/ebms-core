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

import javax.sql.DataSource;
import javax.transaction.SystemException;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.atomikos.icatch.jta.UserTransactionManager;

import bitronix.tm.TransactionManagerServices;
import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.cache.EbMSCacheManager;
import nl.clockwork.ebms.cpa.CPADAO;
import nl.clockwork.ebms.cpa.CertificateMappingDAO;
import nl.clockwork.ebms.cpa.URLMappingDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EbMSEventDAO;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class DAOConfig
{
	public enum TransactionManagerType
	{
		DEFAULT,BITRONIX,ATOMIKOS;
	}
	@Value("${transactionManager.type}")
	TransactionManagerType transactionManagerType;
	@Autowired
	DataSource dataSource;
	@Autowired
	BeanFactory beanFactory;
	@Autowired
	EbMSCacheManager cacheManager;

	@Bean
	public CPADAO cpaDAO() throws Exception
	{
		val result = new ProxyFactoryBean();
		val transactionManager = dataSourceTransactionManager();
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
		result.setBeanFactory(beanFactory);
		result.setTarget(dao);
		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
		return (CPADAO)result.getObject();
	}

	@Bean
	public URLMappingDAO urlMappingDAO() throws Exception
	{
		val result = new ProxyFactoryBean();
		val transactionManager = dataSourceTransactionManager();
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
		result.setBeanFactory(beanFactory);
		result.setTarget(dao);
		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
		return (URLMappingDAO)result.getObject();
	}

	@Bean
	public CertificateMappingDAO certificateMappingDAO() throws Exception
	{
		val result = new ProxyFactoryBean();
		val transactionManager = dataSourceTransactionManager();
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
		result.setBeanFactory(beanFactory);
		result.setTarget(dao);
		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
		return (CertificateMappingDAO)result.getObject();
	}

	@Bean
	public EbMSDAO ebMSDAO() throws Exception
	{
		val transactionTemplate = new TransactionTemplate(dataSourceTransactionManager());
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		val transactionTemplate = new TransactionTemplate(dataSourceTransactionManager());
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSMessageEventDAO)new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSEventDAO ebMSEventDAO() throws Exception
	{
		val transactionTemplate = new TransactionTemplate(dataSourceTransactionManager());
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSEventDAO)new EbMSDAOFactory(transactionManagerType,dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean("dataSourceTransactionManager")
	public PlatformTransactionManager dataSourceTransactionManager() throws SystemException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
			case ATOMIKOS:
				return jtaTransactionManager();
			default:
				return new DataSourceTransactionManager(dataSource);
		}
	}

	@Bean("jtaTransactionManager")
	@DependsOn("btmConfig")
	public PlatformTransactionManager jtaTransactionManager() throws SystemException
	{
		switch (transactionManagerType)
		{
			case BITRONIX:
				val transactionManager = TransactionManagerServices.getTransactionManager();
				return new JtaTransactionManager(transactionManager,transactionManager);
			case ATOMIKOS:
				val userTransactionManager = new UserTransactionManager();
				userTransactionManager.setTransactionTimeout(300);
				userTransactionManager.setForceShutdown(true);
				return new JtaTransactionManager(userTransactionManager,userTransactionManager);
			default:
				return null;
		}
	}

	@Bean("btmConfig")
	public void btmConfig()
	{
		bitronix.tm.Configuration config = TransactionManagerServices.getConfiguration();
		config.setServerId("EbMSTransactionManager");
		config.setDefaultTransactionTimeout(300);
	}

	@Bean(name = "ebMSDAOMethodCachePointCut")
	public RegexpMethodPointcutAdvisor ebMSDAOMethodCachePointCut() throws Exception
	{
		val patterns = new String[]{
				".*existsCPA",
				".*getCPA",
				".*getCPAIds",
				".*existsURLMapping",
				".*getURLMapping",
				".*getURLMappings",
				".*existsCertificateMapping",
				".*getCertificateMapping",
				".*getCertificateMappings"};
		return new RegexpMethodPointcutAdvisor(patterns,ebMSDAOMethodCacheInterceptor());
	}

	@Bean
	public CachingMethodInterceptor ebMSDAOMethodCacheInterceptor() throws Exception
	{
		return cacheManager.getMethodInterceptor("nl.clockwork.ebms.dao.METHOD_CACHE");
	}
}
