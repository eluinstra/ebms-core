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
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
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

@Configuration(proxyBeanMethods = false)
@EnableCaching
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CacheConfig
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@Getter
	public enum CacheType
	{
		NONE(""),
		DEFAULT("nl/clockwork/ebms/ehcache.xml"),
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
			case NONE:
				val s = new SimpleCacheManager();
				Collection<Cache> caches = new ArrayList<>();
				caches.add(createCache("existsCPA"));
				caches.add(createCache("CPA"));
				caches.add(createCache("CPAIds"));
				caches.add(createCache("existsPartyId"));
				caches.add(createCache("getEbMSPartyInfo"));
				caches.add(createCache("getPartyInfo"));
				caches.add(createCache("getFromPartyInfo"));
				caches.add(createCache("getToPartyInfoByFromPartyActionBinding"));
				caches.add(createCache("getToPartyInfo"));
				caches.add(createCache("canSend"));
				caches.add(createCache("canReceive"));
				caches.add(createCache("getDeliveryChannel"));
				caches.add(createCache("getDefaultDeliveryChannel"));
				caches.add(createCache("getSendDeliveryChannel"));
				caches.add(createCache("getReceiveDeliveryChannel"));
				caches.add(createCache("isNonRepudiationRequired"));
				caches.add(createCache("isConfidential"));
				caches.add(createCache("getSyncReply"));
				caches.add(createCache("existsURLMapping"));
				caches.add(createCache("URLMapping"));
				caches.add(createCache("URLMappings"));
				caches.add(createCache("existsCertificateMapping"));
				caches.add(createCache("CertificateMapping"));
				caches.add(createCache("CertificateMappings"));
				s.setCaches(caches);
				return s;
			case IGNITE:
				val ignite = new SpringCacheManager();
				ignite.setConfigurationPath(getConfigLocation().getURL().toString());
				ignite.setDynamicNearCacheConfiguration(createDynamicNearCacheConfiguration());
				return ignite;
			default:
				val ehcache = new EhCacheCacheManager();
				val factory = new EhCacheManagerFactoryBean();
				factory.setConfigLocation(configLocation);
				ehcache.setCacheManager(factory.getObject());
				return ehcache;
		}
	}

	private Cache createCache(String name)
	{
		val result = new ConcurrentMapCacheFactoryBean();
		result.setName(name);
		result.afterPropertiesSet();
		return result.getObject();
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
