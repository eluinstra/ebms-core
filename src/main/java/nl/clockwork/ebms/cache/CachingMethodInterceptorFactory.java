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
