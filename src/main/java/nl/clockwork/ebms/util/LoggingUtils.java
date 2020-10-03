package nl.clockwork.ebms.util;

import java.util.HashMap;
import java.util.Map;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

import lombok.val;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

public class LoggingUtils
{
	public static Map<String,String> getPropertyMap(MessageHeader header)
	{
		val properties = new HashMap<String,String>();
		properties.put("cpaId",header.getCPAId());
		properties.put("fromPartyId",EbMSMessageUtils.toString(header.getFrom().getPartyId()));
		properties.put("fromRole",header.getFrom().getRole());
		properties.put("toPartyId",EbMSMessageUtils.toString(header.getTo().getPartyId()));
		properties.put("toRole",header.getTo().getRole());
		properties.put("service",EbMSMessageUtils.toString(header.getService()));
		properties.put("action",header.getAction());
		properties.put("messageId",header.getMessageData().getMessageId());
		properties.put("conversationId",header.getConversationId());
		properties.put("refToMessageId",header.getMessageData().getRefToMessageId());
		return properties;
	}

	public static Map<String,String> getPropertyMap(EbMSMessageContext context)
	{
		val properties = new HashMap<String,String>();
		properties.put("cpaId",context.getCpaId());
		properties.put("fromPartyId",context.getFromParty().getPartyId());
		properties.put("fromRole",context.getFromParty().getRole());
		properties.put("toPartyId",context.getToParty().getPartyId());
		properties.put("toRole",context.getToParty().getRole());
		properties.put("service",context.getService());
		properties.put("action",context.getAction());
		properties.put("messageId",context.getMessageId());
		properties.put("conversationId",context.getConversationId());
		properties.put("refToMessageId",context.getRefToMessageId());
		return properties;
	}
}
