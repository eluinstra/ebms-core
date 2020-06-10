package nl.clockwork.ebms.cache;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cache.EbMSCacheManager.CacheType;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CacheConfig
{
	@Value("${cache.type}")
	CacheType type;
	@Value("${cache.configLocation}")
	Resource configLocation;

	@Bean
	public EbMSCacheManager ebMSCacheManager() throws IOException
	{
		return new EbMSCacheManager(type,configLocation);
	}
}
