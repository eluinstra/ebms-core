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
package nl.clockwork.ebms.common.dao;

import java.util.List;
import lombok.val;
import nl.clockwork.ebms.api.ebms.model.MessageFilter;

public interface WithMessageFilter
{
	default String getMessageFilter(MessageFilter messageFilter, List<Object> parameters)
	{
		if (messageFilter == null)
			return "";

		val result = new StringBuilder();

		appendEqualsCondition(result, parameters, messageFilter.getCpaId(), "ebms_message.cpa_id");
		appendFromPartyCondition(result, parameters, messageFilter);
		appendToPartyCondition(result, parameters, messageFilter);
		appendEqualsCondition(result, parameters, messageFilter.getService(), "ebms_message.service");
		appendEqualsCondition(result, parameters, messageFilter.getAction(), "ebms_message.action");
		appendEqualsCondition(result, parameters, messageFilter.getConversationId(), "ebms_message.conversation_id");
		appendEqualsCondition(result, parameters, messageFilter.getMessageId(), "ebms_message.message_id");
		appendEqualsCondition(result, parameters, messageFilter.getRefToMessageId(), "ebms_message.ref_to_message_id");

		return result.toString();
	}

	private void appendFromPartyCondition(StringBuilder result, List<Object> parameters, MessageFilter messageFilter)
	{
		if (messageFilter.getFromParty() == null)
			return;

		appendEqualsCondition(result, parameters, messageFilter.getFromParty().getPartyId(), "ebms_message.from_party_id");
		appendEqualsCondition(result, parameters, messageFilter.getFromParty().getRole(), "ebms_message.from_role");
	}

	private void appendToPartyCondition(StringBuilder result, List<Object> parameters, MessageFilter messageFilter)
	{
		if (messageFilter.getToParty() == null)
			return;

		appendEqualsCondition(result, parameters, messageFilter.getToParty().getPartyId(), "ebms_message.to_party_id");
		appendEqualsCondition(result, parameters, messageFilter.getToParty().getRole(), "ebms_message.to_role");
	}

	private void appendEqualsCondition(StringBuilder result, List<Object> parameters, Object value, String column)
	{
		if (value == null)
			return;

		parameters.add(value);
		result.append(" and ").append(column).append(" = ?");
	}
}
