package nl.clockwork.ebms.cache;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CachingMethodInterceptorFactory implements FactoryBean<MethodInterceptor>
{
	@NonNull
	MethodInterceptor methodInterceptor;

	public CachingMethodInterceptorFactory(@NonNull EbMSCacheManager cacheManager, @NonNull String cacheName)
	{
		methodInterceptor = cacheManager.getMethodInterceptor(cacheName);
	}

	@Override
	public MethodInterceptor getObject() throws Exception
	{
		return methodInterceptor;
	}

	@Override
	public Class<?> getObjectType()
	{
		return MethodInterceptor.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}
}
