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
import java.net.URL;

import org.apache.ignite.cache.spring.SpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.experimental.FieldDefaults;
//import nl.clockwork.ebms.cache.EbMSCacheManager.CacheType;

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
		IGNITE("nl/clockwork/ebms/ignite-cache.xml");
		
		String defaultConfigLocation;
	}

	@Value("${cache.type}")
	CacheType type;
	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean//(destroyMethod = "destroy")
	public CacheManager ebMSCacheManager() throws IOException
	{
		//return new EbMSCacheManager(type,configLocation);
		switch (type)
		{
			case NONE:
				return new SimpleCacheManager();
			case IGNITE:
				val ignite = new SpringCacheManager();
				ignite.setConfigurationPath("classpath:nl/clockwork/ebms/ignite.xml");
				return ignite;
			default:
				val ehcache = new EhCacheCacheManager();
				net.sf.ehcache.CacheManager c = new net.sf.ehcache.CacheManager(getClass().getResourceAsStream("/nl/clockwork/ebms/ehcache.xml"));
				ehcache.setCacheManager(c);
				//ehcache.setConfigLocation(new ClassPathResource("nl/clockwork/ebms/ehcache.xml"));
				return ehcache;
				
		}
	}
}
