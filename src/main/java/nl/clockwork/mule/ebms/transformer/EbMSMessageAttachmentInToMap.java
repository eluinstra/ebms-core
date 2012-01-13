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

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.model.EbMSMessage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageAttachmentInToMap extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public EbMSMessageAttachmentInToMap()
	{
		registerSourceType(EbMSMessage.class);
		//FIXME
		//setReturnClass(Map.class);
	}
  
	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			//FIXME get EbMSMessage from payload???
			EbMSMessage msg = (EbMSMessage)message.getProperty(Constants.EBMS_MESSAGE);
			Map<String,Object> map = new HashMap<String,Object>();

			DataSource attachment = msg.getAttachments().get(0);
			if (attachment != null)
			{
				map.put("ebms_message_id","");
				map.put("name",attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName());
				map.put("content_type",attachment.getContentType().split(";")[0].trim());
				map.put("content",IOUtils.toByteArray(attachment.getInputStream()));
			}

			message.setPayload(map);
		}
		catch (Exception e)
		{
			logger.error("",e);
			throw new TransformerException(this,e);
		}
		return message;
	}

}
