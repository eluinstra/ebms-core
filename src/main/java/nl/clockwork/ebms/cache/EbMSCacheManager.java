package nl.clockwork.ebms.cache;

import java.io.IOException;

import org.aopalliance.intercept.MethodInterceptor;
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
		EHCACHE("nl/clockwork/ebms/ehcache.xml"),
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
			case EHCACHE:
				ehcache = CacheManagerBuilder.newCacheManager(new XmlConfiguration(configLocation.getURL()));
				ehcache.init();
				ignite = null;
				break;
			case IGNITE:
				ehcache = null;
				ignite = Ignition.start(configLocation.getURL());
				break;
			default:
				ehcache = null;
				ignite = null;
		}
	}

	public MethodInterceptor getMethodInterceptor(String cacheName)
	{
		switch (type)
		{
			case EHCACHE:
				return new EhCacheMethodCacheInterceptor(ehcache.getCache(cacheName,String.class,Object.class));
			case IGNITE:
				return new JMethodCacheInterceptor(ignite.getOrCreateCache(cacheName));
			default:
				return new DisabledMethodCacheInterceptor();
		}
	}
}
