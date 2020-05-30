package nl.clockwork.ebms.cache;

import java.io.IOException;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.store.LruPolicy;

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
	boolean enableNearCache;
	int maxSize;
	CacheManager ehcache;
	Ignite ignite;

	public EbMSCacheManager(@NonNull CacheType type, Resource configLocation, boolean enableNearCache, int maxSize) throws IOException
	{
		this.type = type;
		configLocation = configLocation == null ? new ClassPathResource(type.defaultConfigLocation) : configLocation;
		this.enableNearCache = enableNearCache;
		this.maxSize = maxSize;
		switch (type)
		{
			case EHCACHE:
				ehcache = CacheManager.newInstance(configLocation.getURL());
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
				return new EhCacheMethodCacheInterceptor(createCache(cacheName));
			case IGNITE:
				return new JMethodCacheInterceptor(createNearCache(cacheName));
			default:
				return new DisabledMethodCacheInterceptor();
		}
	}

	@SuppressWarnings("deprecation")
	private Cache createCache(String cacheName)
	{
		if (!ehcache.cacheExists(cacheName))
			ehcache.addCache(cacheName);
		val result = ehcache.getCache(cacheName);
		if (enableNearCache)
		{
			result.setMemoryStoreEvictionPolicy(new LruPolicy());
			result.getCacheConfiguration().setMemoryStoreEvictionPolicy("LRU");
			result.getCacheConfiguration().setMaxElementsInMemory(maxSize);
		}
		return result;
	}

	@SuppressWarnings("deprecation")
	private javax.cache.Cache<String,Object> createNearCache(String cacheName)
	{
		if (enableNearCache)
		{
			NearCacheConfiguration<String,Object> nearCfg = new NearCacheConfiguration<>();
			nearCfg.setNearEvictionPolicy(new LruEvictionPolicy<>(maxSize));
			return ignite.getOrCreateNearCache(cacheName,nearCfg);
		}
		else
			return ignite.getOrCreateCache(cacheName);
	}
}
