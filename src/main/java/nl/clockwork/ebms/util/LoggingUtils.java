/*
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
package nl.clockwork.ebms.util;

import java.util.HashMap;
import java.util.Map;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSMessageProperties;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils
{
	public enum Status
	{
		DISABLED, ENABLED;
	}

	public static Status mdc;

	public static Map<String,String> getPropertyMap(MessageHeader header)
	{
		val result = new HashMap<String,String>();
		if (header != null)
		{
			result.put("cpaId",header.getCPAId());
			result.put("fromPartyId",EbMSMessageUtils.toString(header.getFrom().getPartyId()));
			result.put("fromRole",header.getFrom().getRole());
			result.put("toPartyId",EbMSMessageUtils.toString(header.getTo().getPartyId()));
			result.put("toRole",header.getTo().getRole());
			result.put("service",EbMSMessageUtils.toString(header.getService()));
			result.put("action",header.getAction());
			result.put("messageId",header.getMessageData().getMessageId());
			result.put("conversationId",header.getConversationId());
			result.put("refToMessageId",header.getMessageData().getRefToMessageId());
		}
		return result;
	}

	public static Map<String,String> getPropertyMap(EbMSMessageProperties properties)
	{
		val result = new HashMap<String,String>();
		if (properties != null)
		{
			result.put("cpaId",properties.getCpaId());
			result.put("fromPartyId",properties.getFromParty().getPartyId());
			result.put("fromRole",properties.getFromParty().getRole());
			result.put("toPartyId",properties.getToParty().getPartyId());
			result.put("toRole",properties.getToParty().getRole());
			result.put("service",properties.getService());
			result.put("action",properties.getAction());
			result.put("messageId",properties.getMessageId());
			result.put("conversationId",properties.getConversationId());
			result.put("refToMessageId",properties.getRefToMessageId());
		}
		return result;
	}
}
