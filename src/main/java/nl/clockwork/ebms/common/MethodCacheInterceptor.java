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

import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import nl.clockwork.ebms.util.CPAUtils;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

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
				if (argument instanceof List)
					handle(sb,(List<?>)argument);
				else
					sb.append(".").append(argument);
		return sb.toString();
	}

	private void handle(StringBuffer sb, List<?> elements)
	{
		if (elements.get(0) instanceof PartyId)
		{
			for (Object partyId : elements)
				sb.append(CPAUtils.toString((PartyId)partyId)).append(",");
			sb.setLength(sb.length()-1);
		}
		else
			sb.append(".").append(StringUtils.join((List<?>)elements,','));
	}

	public void setCache(Cache cache)
	{
		this.cache = cache;
	}

}