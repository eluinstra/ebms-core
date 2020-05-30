package nl.clockwork.ebms.cache;

import java.io.IOException;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.springframework.core.io.Resource;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
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
		EHCACHE("classpath:nl/clockwork/ebms/ehcache.xml"),
		IGNITE("classpath:nl/clockwork/ebms/ignite-cache.xml");
		
		String defaultConfigLocation;
	}

	@NonNull
	CacheType type;
	@NonNull
	Resource configLocation;
	boolean enableNearCache;
	int maxSize;
	CacheManager ehcache;
	Ignite ignite;

	public EbMSCacheManager(@NonNull CacheType type, @NonNull Resource configLocation, boolean enableNearCache, int maxSize) throws CacheException, IOException
	{
		this.type = type;
		this.configLocation = configLocation;
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
				return new IgniteMethodCacheInterceptor(createNearCache(cacheName));
			default:
				return new DisabledMethodCacheInterceptor();
		}
	}

	@SuppressWarnings("deprecation")
	private Cache createCache(String cacheName)
	{
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
	private IgniteCache<String,Object> createNearCache(String cacheName)
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
