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
package nl.clockwork.ebms.plugin.cache.ehcache;

import java.io.IOException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.jcache.JCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@Configuration
@EnableCaching
@PropertySource(value = {"classpath:nl/clockwork/ebms/plugin/cache/ehcache/default.properties"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EhCacheConfig
{
	private static final String DEFAULT_CONFIG_LOCATION = "nl/clockwork/ebms/plugin/cache/ehcache/ehcache.xml";

	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	public JCacheManagerFactoryBean cacheManagerFactoryBean() throws IOException
	{
		JCacheManagerFactoryBean jCacheManagerFactoryBean = new JCacheManagerFactoryBean();
		jCacheManagerFactoryBean.setCacheManagerUri(getConfigLocation().getURI());
		return jCacheManagerFactoryBean;
	}

	@Bean
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
}
