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

import java.io.IOException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.SomeCacheType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
@EnableCaching
@Conditional(SomeCacheType.class)
@PropertySource(value = {"classpath:nl/clockwork/ebms/cache/ehcache/default.properties"}, ignoreResourceNotFound = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EhCacheConfig
{
	private static final String CACHE_TYPE = "EHCACHE";
	private static final String DEFAULT_CONFIG_LOCATION = "nl/clockwork/ebms/cache/ehcache/ehcache.xml";

	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	@Conditional(EhCacheCacheType.class)
	public JCacheManagerFactoryBean cacheManagerFactoryBean() throws IOException
	{
		JCacheManagerFactoryBean jCacheManagerFactoryBean = new JCacheManagerFactoryBean();
		jCacheManagerFactoryBean.setCacheManagerUri(getConfigLocation().getURI());
		return jCacheManagerFactoryBean;
	}

	@Bean
	@Conditional(EhCacheCacheType.class)
	public CacheManager cacheManager(JCacheManagerFactoryBean jCacheManagerFactoryBean)
	{
		final JCacheCacheManager jCacheCacheManager = new JCacheCacheManager();
		jCacheCacheManager.setCacheManager(jCacheManagerFactoryBean.getObject());
		return jCacheCacheManager;
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
			return context.getEnvironment().getProperty("cache.type", String.class, "").equals(CACHE_TYPE);
		}
	}
}
