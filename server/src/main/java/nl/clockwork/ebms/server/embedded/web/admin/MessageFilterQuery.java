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
package nl.clockwork.ebms.server.embedded.web.admin;

import jakarta.ws.rs.QueryParam;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.server.embedded.web.message.EbMSMessageFilter;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageFilterQuery
{
	@QueryParam("cpaId")
	String cpaId;
	@QueryParam("fromPartyId")
	String fromPartyId;
	@QueryParam("fromRole")
	String fromRole;
	@QueryParam("toPartyId")
	String toPartyId;
	@QueryParam("toRole")
	String toRole;
	@QueryParam("service")
	String service;
	@QueryParam("action")
	String action;
	@QueryParam("conversationId")
	String conversationId;
	@QueryParam("messageId")
	String messageId;
	@QueryParam("refToMessageId")
	String refToMessageId;
	@QueryParam("serviceMessage")
	Boolean serviceMessage;
	@QueryParam("status")
	List<EbMSMessageStatus> statuses;
	@QueryParam("from")
	LocalDateTime from;
	@QueryParam("to")
	LocalDateTime to;

	public EbMSMessageFilter toFilter()
	{
		var result = new EbMSMessageFilter();
		result.setCpaId(cpaId);
		result.setFromParty(toParty(fromPartyId, fromRole));
		result.setToParty(toParty(toPartyId, toRole));
		result.setService(service);
		result.setAction(action);
		result.setConversationId(conversationId);
		result.setMessageId(messageId);
		result.setRefToMessageId(refToMessageId);
		result.setServiceMessage(serviceMessage);
		if (statuses != null)
			result.setStatuses(statuses);
		result.setFrom(from);
		result.setTo(to);
		return result;
	}

	private static Party toParty(String partyId, String role)
	{
		return partyId == null ? null : new Party(partyId, role);
	}
}
