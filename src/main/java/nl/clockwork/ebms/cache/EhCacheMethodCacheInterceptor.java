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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.ehcache.Cache;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EhCacheMethodCacheInterceptor implements MethodInterceptor, RemovableCache
{
	@NonNull
	Cache<String,Object> cache;

	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		val targetName = invocation.getThis().getClass().getSimpleName();
		val methodName = invocation.getMethod().getName();
		val arguments = invocation.getArguments();
		val key = getKey(targetName,methodName,arguments);
		if (!cache.containsKey(key))
			cache.put(key,invocation.proceed());
		return cache.get(key);
	}

	public static String getKey(String targetName, String methodName, Object...arguments)
	{
		val sb = new StringBuffer();
		sb.append(targetName).append(".").append(methodName);
		sb.append(Stream.of(arguments).map(a -> String.valueOf(a)).collect(Collectors.joining(",","(",")")));
		return sb.toString();
	}

	@Override
	public void remove(String key)
	{
		cache.remove(key);
	}

	@Override
	public void removeAll()
	{
		cache.clear();
	}
}