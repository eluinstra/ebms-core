package nl.clockwork.ebms.dao;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class MethodCacheInterceptor implements MethodInterceptor
{
	private Cache cache;

	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		String targetName = invocation.getThis().getClass().getName();
		String methodName = invocation.getMethod().getName();
		Object[] arguments = invocation.getArguments();

		String cacheKey = getCacheKey(targetName,methodName,arguments);
		Element element = cache.get(cacheKey);
		if (element == null)
		{
			Object result = invocation.proceed();
			element = new Element(cacheKey,result);
			cache.put(element);
		}
		return element.getObjectValue();
	}

	private String getCacheKey(String targetName, String methodName, Object[] arguments)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		if (arguments != null && arguments.length != 0)
			for (Object argument : arguments)
				sb.append(".").append(argument);
		return sb.toString();
	}

	public void setCache(Cache cache)
	{
		this.cache = cache;
	}

}