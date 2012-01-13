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
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.MessageOrder;
import nl.clockwork.mule.ebms.model.ebxml.SyncReply;
import nl.clockwork.mule.ebms.model.xml.xmldsig.ObjectFactory;
import nl.clockwork.mule.ebms.model.xml.xmldsig.SignatureType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageInToMap extends AbstractMessageAwareTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSDAO ebMSDAO;

	public EbMSMessageInToMap()
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

			map.put("cpa_id",msg.getMessageHeader().getCPAId());
			map.put("conversation_id",msg.getMessageHeader().getConversationId());
			map.put("sequence_nr",0); //TODO use messageOrder
			map.put("message_id",msg.getMessageHeader().getMessageData().getMessageId());
			map.put("ref_to_message_id",msg.getMessageHeader().getMessageData().getRefToMessageId());
			map.put("from_role",msg.getMessageHeader().getFrom().getRole());
			map.put("to_role",msg.getMessageHeader().getTo().getRole());
			map.put("service",msg.getMessageHeader().getService().getValue());
			map.put("action",msg.getMessageHeader().getAction());
			map.put("message_original",msg.getOriginal());
			map.put("message_signature",XMLMessageBuilder.getInstance(SignatureType.class).handle(new ObjectFactory().createSignature(msg.getSignature())));
			//map.put("message_header",XMLUtils.objectToXML(msg.getMessageHeader()));
			map.put("message_header",XMLMessageBuilder.getInstance(MessageHeader.class).handle(msg.getMessageHeader()));
			map.put("message_sync_reply",XMLMessageBuilder.getInstance(SyncReply.class).handle(msg.getSyncReply()));
			map.put("message_order",XMLMessageBuilder.getInstance(MessageOrder.class).handle(msg.getMessageOrder()));
			map.put("message_ack_req",XMLMessageBuilder.getInstance(AckRequested.class).handle(msg.getAckRequested()));
			map.put("message_content",XMLMessageBuilder.getInstance(Manifest.class).handle(msg.getManifest()));
			map.put("status",EbMSMessageStatus.get((String)message.getProperty(Constants.EBMS_MESSAGE_STATUS)).id());
			map.put("status_time",String.format(ebMSDAO.getDefaultDateFormat(),new Date()));

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
