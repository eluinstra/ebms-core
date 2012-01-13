/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.filter;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSMessageActionFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private String action;

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof Map)
		{
				Map map = (Map)message.getPayload();
				return "urn:oasis:names:tc:ebxml-msg:service".equals(map.get("service")) && action.equals(map.get("action"));
		}
		return false;
	}

	public void setAction(String action)
	{
		this.action = action;
	}
}
