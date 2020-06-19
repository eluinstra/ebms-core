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
package nl.clockwork.ebms.cpa;

import javax.sql.DataSource;

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.cache.EbMSCacheManager;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAManagerConfig
{
	@Autowired
	BeanFactory beanFactory;
	@Autowired
	EbMSCacheManager cacheManager;
	@Autowired
	DataSource dataSource;
	@Autowired
	SQLQueryFactory queryFactory;

	@Bean
	@DependsOn("cpaManagerMethodCachePointCut")
	public CPAManager cpaManager() throws Exception
	{
		val result = new ProxyFactoryBean();
		val cpaManager = new CPAManager(cpaDAO(),urlMapper(),cpaMethodCacheInterceptor());
		result.setBeanFactory(beanFactory);
		result.setTarget(cpaManager);
		result.setInterceptorNames("cpaManagerMethodCachePointCut");
		return (CPAManager)result.getObject();
	}

	@Bean(name = "cpaManagerMethodCachePointCut")
	public RegexpMethodPointcutAdvisor cpaManagerMethodCachePointCut() throws Exception
	{
		val patterns = new String[]{
				".*existsCPA",
				".*getCPA",
				".*getCPAIds",
				".*existsPartyId",
				".*getEbMSPartyInfo",
				".*getPartyInfo",
				".*getFromPartyInfo",
				".*getToPartyInfoByFromPartyActionBinding",
				".*getToPartyInfo",
				".*canSend",
				".*canReceive",
				".*getDeliveryChannel",
				".*getDefaultDeliveryChannel",
				".*getSendDeliveryChannel",
				".*getReceiveDeliveryChannel",
				".*isNonRepudiationRequired",
				".*isConfidential",
				//".*getUri,
				".*getSyncReply"};
		return new RegexpMethodPointcutAdvisor(patterns,cpaMethodCacheInterceptor());
	}

	@Bean
	public CachingMethodInterceptor cpaMethodCacheInterceptor() throws Exception
	{
		return cacheManager.getMethodInterceptor("nl.clockwork.ebms.cpa.METHOD_CACHE");
	}

	@Bean
	public URLMapper urlMapper() throws Exception
	{
		return new URLMapper(ebMSDAOMethodCacheInterceptor(),urlMappingDAO());
	}

	@Bean
	public CertificateMapper certificateMapper() throws Exception
	{
		return new CertificateMapper(ebMSDAOMethodCacheInterceptor(),certificateMappingDAO());
	}

	@Bean
	public CPADAO cpaDAO() throws Exception
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new CPADAOImpl(jdbcTemplate,queryFactory);
	}

	@Bean
	public URLMappingDAO urlMappingDAO() throws Exception
	{
//		val result = new ProxyFactoryBean();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new URLMappingDAOImpl(jdbcTemplate,queryFactory);
		return dao;
//		result.setBeanFactory(beanFactory);
//		result.setTarget(dao);
//		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
//		return (URLMappingDAO)result.getObject();
	}

	@Bean
	public CertificateMappingDAO certificateMappingDAO() throws Exception
	{
//		val result = new ProxyFactoryBean();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		val dao = new CertificateMappingDAOImpl(jdbcTemplate,queryFactory);
		return dao;
//		result.setBeanFactory(beanFactory);
//		result.setTarget(dao);
//		result.setInterceptorNames("ebMSDAOMethodCachePointCut");
//		return (CertificateMappingDAO)result.getObject();
	}

	@Bean(name = "ebMSDAOMethodCachePointCut")
	public RegexpMethodPointcutAdvisor ebMSDAOMethodCachePointCut() throws Exception
	{
		val patterns = new String[]{
//				".*existsCPA",
//				".*getCPA",
//				".*getCPAIds",
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
