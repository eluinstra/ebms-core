package nl.clockwork.ebms.cpa;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.cache.CachingMethodInterceptorFactory;
import nl.clockwork.ebms.cache.EbMSCacheManager;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingDAO;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingDAOImpl;
import nl.clockwork.ebms.cpa.certificate.CertificateMappingMapper;
import nl.clockwork.ebms.cpa.url.URLMappingDAO;
import nl.clockwork.ebms.cpa.url.URLMappingDAOImpl;
import nl.clockwork.ebms.cpa.url.URLMappingMapper;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPADAOConfig
{
	@Autowired
	SqlSessionFactory sqlSessionFactory;
	@Autowired
	BeanFactory beanFactory;
	@Autowired
	EbMSCacheManager cacheManager;

	@Bean
	public CPADAO cpaDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<CPAMapper>(CPAMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return (CPADAO)createMethodCacheProxy(new CPADAOImpl(factoryBean.getObject()));
	}

	@Bean
	public URLMappingDAO urlMappingDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<URLMappingMapper>(URLMappingMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return (URLMappingDAO)createMethodCacheProxy(new URLMappingDAOImpl(factoryBean.getObject()));
	}

	@Bean
	public CertificateMappingDAO certificateMappingDAO() throws Exception
	{
		val factoryBean = new MapperFactoryBean<CertificateMappingMapper>(CertificateMappingMapper.class);
		factoryBean.setSqlSessionFactory(sqlSessionFactory);
		return (CertificateMappingDAO)createMethodCacheProxy(new CertificateMappingDAOImpl(factoryBean.getObject()));
	}

	@Bean(name = "daoMethodCachePointCut")
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

	private Object createMethodCacheProxy(Object o)
	{
		val result = new ProxyFactoryBean();
		result.setBeanFactory(beanFactory);
		result.setTarget(o);
		result.setInterceptorNames("daoMethodCachePointCut");
		return result.getObject();
	}
}
