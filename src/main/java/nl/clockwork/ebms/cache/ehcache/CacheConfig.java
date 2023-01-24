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
package nl.clockwork.ebms.cache.ehcache;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.cache.SomeCacheType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@EnableCaching
@Conditional(SomeCacheType.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CacheConfig
{
	private static final String CACHE_TYPE = "EHCACHE";
	private static final String DEFAULT_CONFIG_LOCATION = "nl/clockwork/ebms/ehcache.xml";

	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	@Conditional(EhCacheCacheType.class)
	public CacheManager ehcacheCacheManager()
	{
		val result = new EhCacheCacheManager();
		result.setCacheManager(createCacheManager());
		return result;
	}

	private net.sf.ehcache.CacheManager createCacheManager()
	{
		val result = createEhCacheManager(getConfigLocation());
		result.addCache("CPA");
		result.addCache("URLMapping");
		result.addCache("CertificateMapping");
		return result;
	}

	private net.sf.ehcache.CacheManager createEhCacheManager(Resource configLocation)
	{
		val ehCacheManagerFactory = new EhCacheManagerFactoryBean();
		ehCacheManagerFactory.setConfigLocation(configLocation);
		ehCacheManagerFactory.afterPropertiesSet();
		return ehCacheManagerFactory.getObject();
	}

	private Resource getConfigLocation()
	{
		return configLocation == null ? new ClassPathResource(DEFAULT_CONFIG_LOCATION) : configLocation;
	}

	public static class EhCacheCacheType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("cache.type",String.class,"").equals(CACHE_TYPE);
		}
	}
}
