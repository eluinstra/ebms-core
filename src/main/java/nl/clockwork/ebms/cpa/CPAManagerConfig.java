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

import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.RegexpMethodPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.cache.CachingMethodInterceptorFactory;
import nl.clockwork.ebms.cache.EbMSCacheManager;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CPAManagerConfig
{
	@Autowired
	CPADAO cpaDAO;
	@Autowired
	@Qualifier("ebMSDAOMethodCacheInterceptor")
	CachingMethodInterceptor daoMethodCache;
	@Autowired
	BeanFactory beanFactory;
	@Autowired
	EbMSCacheManager cacheManager;
	@Autowired
	URLMappingDAO urlMappingDAO;
	@Autowired
	CertificateMappingDAO certificateMappingDAO;

	@Bean
	@DependsOn("cpaManagerMethodCachePointCut")
	public CPAManager cpaManager() throws Exception
	{
		val result = new ProxyFactoryBean();
		val cpaManager = new CPAManager(daoMethodCache,cpaMethodCacheInterceptor(),cpaDAO,urlMapper());
		result.setBeanFactory(beanFactory);
		result.setTarget(cpaManager);
		result.setInterceptorNames("cpaManagerMethodCachePointCut");
		return (CPAManager)result.getObject();
	}

	@Bean(name = "cpaManagerMethodCachePointCut")
	public RegexpMethodPointcutAdvisor cpaManagerMethodCachePointCut() throws Exception
	{
		val patterns = new String[]{
				//".*existsCPA",
				//".*getCPA",
				//".*getCPAIds,
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
		return new CachingMethodInterceptorFactory(cacheManager,"nl.clockwork.ebms.cpa.METHOD_CACHE").getObject();
	}

	@Bean
	public URLMapper urlMapper()
	{
		return new URLMapper(daoMethodCache,urlMappingDAO);
	}

	@Bean
	public CertificateMapper certificateMapper()
	{
		return new CertificateMapper(daoMethodCache,certificateMappingDAO);
	}
}
