package nl.clockwork.ebms.dao;

import javax.sql.DataSource;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.cache.CachingMethodInterceptorFactory;
import nl.clockwork.ebms.cache.EbMSCacheManager;
import nl.clockwork.ebms.cpa.CPADAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EbMSEventDAO;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class DAOConfig
{
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
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
		result.setBeanFactory(beanFactory);
		result.setTarget(dao);
		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
		return (CPADAO)result.getObject();
	}

	@Bean
	public EbMSDAO ebMSDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSMessageEventDAO)new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
	}

	@Bean
	public EbMSEventDAO ebMSEventDAO() throws Exception
	{
		val transactionManager = new DataSourceTransactionManager(dataSource);
		val transactionTemplate = new TransactionTemplate(transactionManager);
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return (EbMSEventDAO)new EbMSDAOFactory(dataSource,transactionTemplate,jdbcTemplate).getObject();
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
		return new CachingMethodInterceptorFactory(cacheManager,"nl.clockwork.ebms.dao.METHOD_CACHE").getObject();
	}
}
