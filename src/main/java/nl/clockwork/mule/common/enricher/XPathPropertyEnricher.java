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
package nl.clockwork.mule.common.enricher;

import java.util.Map;

import nl.clockwork.ebms.common.util.XMLUtils;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class XPathPropertyEnricher extends AbstractMessageAwareTransformer
{
	private Map<String,String> xpathQueries;

	public XPathPropertyEnricher()
	{
		registerSourceType(String.class);
		//FIXME
		//setReturnClass(String.class);
	}
	
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		String content = (String)message.getPayload();
		for (String name : xpathQueries.keySet())
		{
			try
			{
				message.setProperty(name,XMLUtils.executeXPathQuery(content,xpathQueries.get(name)));
			}
			catch (Exception e)
			{
				message.setProperty(name,null);
			}
			
		}
		return message;
	}

	public void setXpathQueries(Map<String,String> xpathQueries)
	{
		this.xpathQueries = xpathQueries;
	}
}
