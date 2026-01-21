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
package nl.clockwork.ebms.plugin.cache.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import java.io.IOException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;

@Configuration
@EnableCaching(proxyTargetClass = true)
@PropertySource(value = {"classpath:nl/clockwork/ebms/plugin/cache/hazelcast/default.properties"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HazelcastConfig
{
	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	public HazelcastInstance hazelcastInstance() throws IOException
	{
		val config = configLocation == null ? Config.load() : Config.loadFromFile(configLocation.getFile());
		return Hazelcast.newHazelcastInstance(config);
	}

	@Bean
	public CacheManager cacheManager(HazelcastInstance hazelcastInstance)
	{
		return new HazelcastCacheManager(hazelcastInstance);
	}
}
