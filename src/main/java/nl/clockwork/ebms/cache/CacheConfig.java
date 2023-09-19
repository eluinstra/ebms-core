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
package nl.clockwork.ebms.cache;

import java.io.IOException;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicyFactory;
import org.apache.ignite.cache.spring.SpringCacheManager;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
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
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	public static enum CacheType
	{
		NONE(""), DEFAULT(""), EHCACHE("nl/clockwork/ebms/ehcache.xml"), IGNITE("nl/clockwork/ebms/ignite.xml");

		String defaultConfigLocation;
	}

	@Value("${cache.type}")
	CacheType type;
	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	@Conditional(DefaultCacheType.class)
	public CacheManager simpleCacheManager()
	{
		val result = new SimpleCacheManager();
		val caches = new ArrayList<Cache>();
		caches.add(new ConcurrentMapCache("CPA"));
		caches.add(new ConcurrentMapCache("URLMapping"));
		caches.add(new ConcurrentMapCache("CertificateMapping"));
		result.setCaches(caches);
		return result;
	}

	@Bean
	@Conditional(EhCacheCacheType.class)
	public CacheManager ehcacheCacheManager()
	{
		val result = new EhCacheCacheManager();
		val ehcacheManager = createEhCacheManager(getConfigLocation());
		ehcacheManager.addCache("CPA");
		ehcacheManager.addCache("URLMapping");
		ehcacheManager.addCache("CertificateMapping");
		result.setCacheManager(ehcacheManager);
		return result;
	}

	@Bean
	@Conditional(IgniteCacheType.class)
	public CacheManager igniteCacheManager() throws IOException
	{
		val result = new SpringCacheManager();
		result.setConfigurationPath(getConfigLocation().getURL().toString());
		result.setDynamicNearCacheConfiguration(createDynamicNearCacheConfiguration());
		return result;
	}

	@Bean("ebMSKeyGenerator")
	public KeyGenerator keyGenerator()
	{
		return new EbMSKeyGenerator();
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
		return configLocation == null ? new ClassPathResource(type.defaultConfigLocation) : configLocation;
	}

	private NearCacheConfiguration<Object, Object> createDynamicNearCacheConfiguration()
	{
		val result = new NearCacheConfiguration<Object, Object>();
		result.setNearEvictionPolicyFactory(createNearEvictPlcFactory());
		return result;
	}

	private LruEvictionPolicyFactory<Object, Object> createNearEvictPlcFactory()
	{
		val result = new LruEvictionPolicyFactory<Object, Object>();
		result.setMaxSize(100000);
		return result;
	}

	public static class DefaultCacheType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("cache.type", CacheType.class, CacheType.DEFAULT) == CacheType.DEFAULT;
		}
	}

	public static class EhCacheCacheType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("cache.type", CacheType.class, CacheType.DEFAULT) == CacheType.EHCACHE;
		}
	}

	public static class IgniteCacheType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("cache.type", CacheType.class, CacheType.DEFAULT) == CacheType.IGNITE;
		}
	}
}
