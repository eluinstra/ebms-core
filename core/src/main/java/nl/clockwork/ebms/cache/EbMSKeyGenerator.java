/*
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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import nl.clockwork.ebms.cpa.CPAUtils;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.springframework.cache.interceptor.KeyGenerator;

public class EbMSKeyGenerator implements KeyGenerator
{
	@Override
	public Object generate(Object target, Method method, Object...params)
	{
		return method.getName() + "[" + join(params, ",") + "]";
	}

	private String join(Object[] objects, String delimiter)
	{
		return stream(objects).map(o -> toString(o)).collect(joining(delimiter));
	}

	private String toString(Object o)
	{
		if (o == null)
		{
			return "";
		}
		else if (o instanceof String s)
		{
			return s;
		}
		else if (o instanceof List)
		{
			return toString((List<PartyId>)o);
		}
		else
		{
			return "";
		}
	}

	private String toString(List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyId)
	{
		return partyId.stream().map(CPAUtils::toString).collect(Collectors.joining(","));
	}
}
