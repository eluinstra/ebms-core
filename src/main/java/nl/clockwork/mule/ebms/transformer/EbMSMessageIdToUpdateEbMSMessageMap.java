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
package nl.clockwork.mule.ebms.transformer;

import java.util.HashMap;
import java.util.Map;

import nl.clockwork.mule.ebms.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageIdToUpdateEbMSMessageMap extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private Map<String,Object> parameters = new HashMap<String,Object>();
  
  public EbMSMessageIdToUpdateEbMSMessageMap()
	{
		//registerSourceType(Object.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			HashMap<String,Object> map = new HashMap<String,Object>();
			map.put("id",message.getLongProperty(Constants.EBMS_MESSAGE_ID,0));
			for (String key : parameters.keySet())
				map.put(key,parameters.get(key));
			message.setPayload(map);
		}
		catch (Exception e)
		{
			logger.error("",e);
			throw new TransformerException(this,e);
		}
		return message;
	}

	public void setParameters(Map<String,Object> parameters)
	{
		this.parameters = parameters;
	}
}
