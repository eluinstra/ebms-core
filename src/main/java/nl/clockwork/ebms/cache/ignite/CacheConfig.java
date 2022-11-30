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
package nl.clockwork.ebms.cache.ignite;

import java.io.IOException;

import org.apache.ignite.cache.eviction.lru.LruEvictionPolicyFactory;
import org.apache.ignite.cache.spring.SpringCacheManager;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.SomeCacheType;

@Configuration
@EnableCaching
@Conditional(SomeCacheType.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CacheConfig
{
	private static final String CACHE_TYPE = "IGNITE";
	private static final String DEFAULT_CONFIG_LOCATION = "nl/clockwork/ebms/ignite.xml";

	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	@Conditional(IgniteCacheType.class)
	public CacheManager igniteCacheManager() throws IOException
	{
		val result = new SpringCacheManager();
		result.setConfigurationPath(getConfigLocation().getURL().toString());
		result.setDynamicNearCacheConfiguration(createDynamicNearCacheConfiguration());
		return result;
	}

  private Resource getConfigLocation()
	{
		return configLocation == null ? new ClassPathResource(DEFAULT_CONFIG_LOCATION) : configLocation;
	}

	private NearCacheConfiguration<Object,Object> createDynamicNearCacheConfiguration()
	{
		val result = new NearCacheConfiguration<Object,Object>();
		result.setNearEvictionPolicyFactory(createNearEvictPlcFactory());
		return result;
	}

	private LruEvictionPolicyFactory<Object,Object> createNearEvictPlcFactory()
	{
		val result = new LruEvictionPolicyFactory<Object,Object>();
		result.setMaxSize(100000);
		return result;
	}

	public static class IgniteCacheType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("cache.type",String.class,"").equals(CACHE_TYPE);
		}
	}
}
