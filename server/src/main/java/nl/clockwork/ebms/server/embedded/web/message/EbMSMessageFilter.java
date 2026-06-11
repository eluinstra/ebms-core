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
package nl.clockwork.ebms.server.embedded.web.message;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.api.ebms.model.MessageFilter;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.EbMSMessageStatus;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings("java:S2160")
public class EbMSMessageFilter extends MessageFilter
{
	private static final long serialVersionUID = 1L;
	Boolean serviceMessage;
	List<EbMSMessageStatus> statuses = Collections.emptyList();
	LocalDateTime from;
	LocalDateTime to;

	@Builder(builderMethodName = "ebMSMessageFilterBuilder")
	public EbMSMessageFilter(
			@NonNull String cpaId,
			@NonNull Party fromParty,
			Party toParty,
			String service,
			String action,
			Instant timestamp,
			String conversationId,
			String messageId,
			String refToMessageId,
			EbMSMessageStatus messageStatus,
			Boolean serviceMessage,
			List<EbMSMessageStatus> statuses,
			LocalDateTime from,
			LocalDateTime to)
	{
		super(cpaId, fromParty, toParty, service, action, conversationId, messageId, refToMessageId);
		this.serviceMessage = serviceMessage;
		if (statuses != null)
			this.statuses = statuses;
		this.from = from;
		this.to = to;
	}
}
