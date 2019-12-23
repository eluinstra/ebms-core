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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class MethodCacheInterceptor implements MethodInterceptor
{
	private Cache cache;

	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		String targetName = invocation.getThis().getClass().getSimpleName();
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

	public static String getCacheKey(String targetName, String methodName, Object...arguments)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		sb.append(Stream.of(arguments).map(a -> String.valueOf(a)).collect(Collectors.joining(",","(",")")));
		return sb.toString();
	}

	public void setCache(Cache cache)
	{
		this.cache = cache;
	}

}