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
package nl.clockwork.ebms.server.embedded.dao;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.val;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.protocol.EbMSAction;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.server.message.model.embedded.web.EbMSMessageFilter;

public interface WithMessageFilter
{
	default String getMessageFilter(EbMSMessageFilter filter, List<Object> parameters)
	{
		if (filter == null)
			return "";
		val result = new StringBuilder();
		appendEquals(result, parameters, filter.getCpaId(), "cpa_id");
		appendParty(result, parameters, filter.getFromParty(), "from_party_id", "from_role");
		appendParty(result, parameters, filter.getToParty(), "to_party_id", "to_role");
		appendEquals(result, parameters, filter.getService(), "service");
		appendEquals(result, parameters, filter.getAction(), "action");
		appendEquals(result, parameters, filter.getConversationId(), "conversation_id");
		appendEquals(result, parameters, filter.getMessageId(), "message_id");
		appendEquals(result, parameters, filter.getRefToMessageId(), "ref_to_message_id");
		appendStatuses(result, filter.getStatuses());
		appendServiceMessage(result, filter.getServiceMessage());
		appendTimestampRange(result, parameters, filter);
		return result.toString();
	}

	private static void appendEquals(StringBuilder result, List<Object> parameters, Object value, String column)
	{
		if (value == null)
			return;
		parameters.add(value);
		result.append(" and ").append(column).append(" = ?");
	}

	private static void appendParty(StringBuilder result, List<Object> parameters, Party party, String partyIdColumn, String roleColumn)
	{
		if (party == null)
			return;
		appendEquals(result, parameters, party.getPartyId(), partyIdColumn);
		appendEquals(result, parameters, party.getRole(), roleColumn);
	}

	private static void appendStatuses(StringBuilder result, List<EbMSMessageStatus> statuses)
	{
		if (statuses == null || statuses.isEmpty())
			return;
		result.append(" and status in (").append(statuses.stream().map(EbMSMessageStatus::getId).map(String::valueOf).collect(Collectors.joining(","))).append(")");
	}

	private static void appendServiceMessage(StringBuilder result, Boolean serviceMessage)
	{
		if (serviceMessage == null)
			return;
		result.append(" and service ").append(Boolean.TRUE.equals(serviceMessage) ? "= '" : "<> '").append(EbMSAction.EBMS_SERVICE_URI).append("'");
	}

	private static void appendTimestampRange(StringBuilder result, List<Object> parameters, EbMSMessageFilter filter)
	{
		if (filter.getFrom() != null)
		{
			parameters.add(Timestamp.from(filter.getFrom().atZone(ZoneId.systemDefault()).toInstant()));
			result.append(" and time_stamp >= ?");
		}
		if (filter.getTo() != null)
		{
			parameters.add(Timestamp.from(filter.getTo().atZone(ZoneId.systemDefault()).toInstant()));
			result.append(" and time_stamp < ?");
		}
	}

	default String joinStatusIds(EbMSMessageStatus[] statuses)
	{
		return Stream.of(statuses).map(EbMSMessageStatus::getId).map(String::valueOf).collect(Collectors.joining(","));
	}
}
