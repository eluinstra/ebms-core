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

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class MapPropertyEnricher extends AbstractMessageAwareTransformer
{
	private Map<String,String> keys;

	public MapPropertyEnricher()
	{
		registerSourceType(Map.class);
		//FIXME
		//setReturnClass(Map.class);
	}
	
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
        Map<String,Object> map = (Map<String,Object>)message.getPayload();
		for (String key : keys.keySet())
		{
			message.setProperty(keys.get(key),map.get(key));
		}
		return message;
	}

	public void setKeys(Map<String,String> keys)
	{
		this.keys = keys;
	}
}
