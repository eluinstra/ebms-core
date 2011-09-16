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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToInsertEbMSMessageMap extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;

	public EbMSMessageToInsertEbMSMessageMap()
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
			EbMSMessage msg = (EbMSMessage)message.getPayload();
			Date date = new Date();
			Map<String,Object> map = new HashMap<String,Object>();

			map.put("time_stamp",String.format(ebMSDAO.getDefaultDateFormat(),date));
			map.put("cpa_id",msg.getMessageHeader().getCPAId());
			map.put("conversation_id",msg.getMessageHeader().getConversationId());
			map.put("sequence_nr",0);

			map.put("message_id",msg.getMessageHeader().getMessageData().getMessageId());
			map.put("message_type",Constants.EbMSMessageType.IN.id());
			map.put("message_original",msg.getMessage());
			//map.put("message_header",XMLUtils.objectToXML(msg.getMessageHeader()));
			map.put("message_header",XMLMessageBuilder.getInstance(MessageHeader.class).handle(msg.getMessageHeader()));
			//map.put("message_manifest",XMLUtils.objectToXML(msg.getManifest()));
			map.put("message_manifest",XMLMessageBuilder.getInstance(Manifest.class).handle(msg.getManifest()));

			map.put("status",Constants.EbMSMessageStatus.RECEIVED.id());

			message.setPayload(map);
		}
		catch (Exception e)
		{
			logger.error("",e);
			throw new TransformerException(this,e);
		}
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
