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
package nl.clockwork.ebms.common;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ehcache.Cache;

public class MethodCacheInterceptor implements MethodInterceptor
{
	private Cache<String,Object> cache;

	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		String targetName = invocation.getThis().getClass().getSimpleName();
		String methodName = invocation.getMethod().getName();
		Object[] arguments = invocation.getArguments();

		String cacheKey = getCacheKey(targetName,methodName,arguments);
		Object element = cache.get(cacheKey);
		if (element == null)
		{
			Object result = invocation.proceed();
			cache.put(cacheKey,result);
		}
		return element;
	}

	public static String getCacheKey(String targetName, String methodName, Object...arguments)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		if (arguments != null && arguments.length != 0)
			for (Object argument : arguments)
				sb.append(".").append(argument);
		return sb.toString();
	}

	public void setCache(Cache<String,Object> cache)
	{
		this.cache = cache;
	}

}