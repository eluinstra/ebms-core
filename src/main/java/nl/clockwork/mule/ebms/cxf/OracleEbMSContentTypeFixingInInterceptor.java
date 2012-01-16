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
package nl.clockwork.mule.ebms.cxf;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.mule.DefaultMuleMessage;
import org.mule.api.transport.PropertyScope;

public class OracleEbMSContentTypeFixingInInterceptor extends AbstractPhaseInterceptor<Message>
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public OracleEbMSContentTypeFixingInInterceptor()
	{
		super(Phase.RECEIVE);
		addBefore(LoggingInInterceptor.class.getName());
	}

	public OracleEbMSContentTypeFixingInInterceptor(String phase)
	{
		super(phase);
	}

	public void handleMessage(Message message) throws Fault
	{
		DefaultMuleMessage m = (DefaultMuleMessage)message.get("mule.message");
//		String s = (String)m.getAdapter().getProperty("Content-type");
//		if (StringUtils.isNotEmpty(s))
//			message.put(Message.CONTENT_TYPE,s);
		for (Object name : m.getAdapter().getPropertyNames())
			if (((String)name).equalsIgnoreCase("content-type"))
			{
				String s = (String)m.getAdapter().getProperty((String)name,PropertyScope.INBOUND);
				if (StringUtils.isNotBlank(s))
				{
					message.put(Message.CONTENT_TYPE,s);
					break;
				}
			}
	}

}
