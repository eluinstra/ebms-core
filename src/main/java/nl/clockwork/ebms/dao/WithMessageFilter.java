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
package nl.clockwork.ebms.dao;

import java.util.List;

import lombok.val;
import nl.clockwork.ebms.service.model.MessageFilter;

public interface WithMessageFilter {
	default String getMessageFilter(MessageFilter messageFilter, List<Object> parameters)
	{
		val result = new StringBuilder();
		if (messageFilter != null)
		{
			if (messageFilter.getCpaId() != null)
			{
				parameters.add(messageFilter.getCpaId());
				result.append(" and ebms_message.cpa_id = ?");
			}
			if (messageFilter.getFromParty() != null)
			{
				if (messageFilter.getFromParty().getPartyId() != null)
				{
					parameters.add(messageFilter.getFromParty().getPartyId());
					result.append(" and ebms_message.from_party_id = ?");
				}
				if (messageFilter.getFromParty().getRole() != null)
				{
					parameters.add(messageFilter.getFromParty().getRole());
					result.append(" and ebms_message.from_role = ?");
				}
			}
			if (messageFilter.getToParty() != null)
			{
				if (messageFilter.getToParty().getPartyId() != null)
				{
					parameters.add(messageFilter.getToParty().getPartyId());
					result.append(" and ebms_message.to_party_id = ?");
				}
				if (messageFilter.getToParty().getRole() != null)
				{
					parameters.add(messageFilter.getToParty().getRole());
					result.append(" and ebms_message.to_role = ?");
				}
			}
			if (messageFilter.getService() != null)
			{
				parameters.add(messageFilter.getService());
				result.append(" and ebms_message.service = ?");
			}
			if (messageFilter.getAction() != null)
			{
				parameters.add(messageFilter.getAction());
				result.append(" and ebms_message.action = ?");
			}
			if (messageFilter.getConversationId() != null)
			{
				parameters.add(messageFilter.getConversationId());
				result.append(" and ebms_message.conversation_id = ?");
			}
			if (messageFilter.getMessageId() != null)
			{
				parameters.add(messageFilter.getMessageId());
				result.append(" and ebms_message.message_id = ?");
			}
			if (messageFilter.getRefToMessageId() != null)
			{
				parameters.add(messageFilter.getRefToMessageId());
				result.append(" and ebms_message.ref_to_message_id = ?");
			}
		}
		return result.toString();
	}
}
