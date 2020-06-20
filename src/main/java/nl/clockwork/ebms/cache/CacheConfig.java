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
package nl.clockwork.ebms.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableCaching
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CacheConfig
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	public enum CacheType
	{
		DEFAULT(""),
		EHCACHE("nl/clockwork/ebms/ehcache.xml"),
		IGNITE("nl/clockwork/ebms/ignite.xml");
		
		String defaultConfigLocation;
	}

	@Value("${cache.type}")
	CacheType type;
	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	public CacheManager ebMSCacheManager() throws IOException
	{
		switch (type)
		{
			case EHCACHE:
				val ehcacheCacheManager = new EhCacheCacheManager();
				net.sf.ehcache.CacheManager ehcacheManager = createEhCacheManager(getConfigLocation());
				ehcacheManager.addCache("CPA");
				ehcacheManager.addCache("URLMapping");
				ehcacheManager.addCache("CertificateMapping");
				ehcacheCacheManager.setCacheManager(ehcacheManager);
				return ehcacheCacheManager;
			case IGNITE:
				val igniteCacheManager = new SpringCacheManager();
				igniteCacheManager.setConfigurationPath(getConfigLocation().getURL().toString());
				igniteCacheManager.setDynamicNearCacheConfiguration(createDynamicNearCacheConfiguration());
				return igniteCacheManager;
			default:
				val cacheManager = new SimpleCacheManager();
				Collection<Cache> caches = new ArrayList<>();
				caches.add(new ConcurrentMapCache("CPA"));
				caches.add(new ConcurrentMapCache("URLMapping"));
				caches.add(new ConcurrentMapCache("CertificateMapping"));
				cacheManager.setCaches(caches);
				return cacheManager;
		}
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
		net.sf.ehcache.CacheManager ehcacheManager = ehCacheManagerFactory.getObject();
		return ehcacheManager;
	}

  private Resource getConfigLocation()
	{
		return configLocation == null ? new ClassPathResource(type.defaultConfigLocation) : configLocation;
	}

	private NearCacheConfiguration<Object,Object> createDynamicNearCacheConfiguration()
	{
		val result = new NearCacheConfiguration<Object,Object>();
		LruEvictionPolicyFactory<Object,Object> nearEvictPlcFactory = new LruEvictionPolicyFactory<Object,Object>();
		nearEvictPlcFactory.setMaxSize(100000);
		result.setNearEvictionPolicyFactory(nearEvictPlcFactory);
		return result;
	}
}
