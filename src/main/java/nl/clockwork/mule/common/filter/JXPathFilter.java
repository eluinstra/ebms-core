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
package nl.clockwork.mule.common.filter;

import org.apache.commons.jxpath.JXPathContext;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class JXPathFilter implements Filter
{
	private String xPathQuery;
	private String regEx;
	
	@Override
	public boolean accept(MuleMessage message)
	{
		try
		{
			JXPathContext context = JXPathContext.newContext(message.getPayload());
			String s = context.getValue(xPathQuery).toString();
			return s.matches(regEx);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void setxPathQuery(String xPathQuery)
	{
		this.xPathQuery = xPathQuery;
	}

	public void setRegEx(String regEx)
	{
		this.regEx = regEx;
	}
}
