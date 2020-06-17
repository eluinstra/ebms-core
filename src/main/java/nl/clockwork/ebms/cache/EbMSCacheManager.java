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

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSCacheManager
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

	@NonNull
	CacheType type;
	CacheManager ehcache;
	Ignite ignite;

	public EbMSCacheManager(@NonNull CacheType type, Resource configLocation) throws IOException
	{
		this.type = type;
		configLocation = configLocation == null ? new ClassPathResource(type.defaultConfigLocation) : configLocation;
		switch (type)
		{
			case NONE:
				ehcache = null;
				ignite = null;
				break;
			case IGNITE:
				ehcache = null;
				ignite = Ignition.start(configLocation.getURL());
				break;
			default:
				ehcache = CacheManagerBuilder.newCacheManager(new XmlConfiguration(configLocation.getURL()));
				ehcache.init();
				ignite = null;
		}
	}

	public CachingMethodInterceptor getMethodInterceptor(String cacheName)
	{
		switch (type)
		{
			case DEFAULT:
				return new EhCacheMethodCacheInterceptor(ehcache.getCache(cacheName,String.class,Object.class));
			case IGNITE:
				return new JMethodCacheInterceptor(ignite.getOrCreateCache(cacheName));
			default:
				return new DisabledMethodCacheInterceptor();
		}
	}
}
