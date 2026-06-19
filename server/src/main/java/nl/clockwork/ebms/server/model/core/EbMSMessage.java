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
package nl.clockwork.ebms.server.message.model.core;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.DOMUtils;
import org.w3c.dom.Document;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class EbMSMessage implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NonNull
	Instant timestamp;
	@NonNull
	String cpaId;
	@NonNull
	String conversationId;
	@NonNull
	String messageId;
	String refToMessageId;
	Instant timeToLive;
	@NonNull
	String fromPartyId;
	String fromRole;
	@NonNull
	String toPartyId;
	String toRole;
	@NonNull
	String service;
	@NonNull
	String action;
	String content;
	EbMSMessageStatus status;
	Instant statusTime;
	@NonNull
	List<EbMSAttachment> attachments = Collections.emptyList();
	DeliveryTask deliveryTask;
	@NonNull
	List<DeliveryLog> deliveryLogs = Collections.emptyList();

	public EbMSMessage(
			@NonNull Instant timestamp,
			@NonNull String cpaId,
			@NonNull String conversationId,
			@NonNull String messageId,
			String refToMessageId,
			Instant timeToLive,
			@NonNull String fromPartyId,
			String fromRole,
			@NonNull String toPartyId,
			String toRole,
			@NonNull String service,
			@NonNull String action,
			EbMSMessageStatus status,
			Instant statusTime)
	{
		this.timestamp = timestamp;
		this.cpaId = cpaId;
		this.conversationId = conversationId;
		this.messageId = messageId;
		this.refToMessageId = refToMessageId;
		this.timeToLive = timeToLive;
		this.fromPartyId = fromPartyId;
		this.fromRole = fromRole;
		this.toPartyId = toPartyId;
		this.toRole = toRole;
		this.service = service;
		this.action = action;
		this.status = status;
		this.statusTime = statusTime;
		this.attachments = Collections.emptyList();
		this.deliveryLogs = Collections.emptyList();
	}

	public EbMSMessage(
			@NonNull Instant timestamp,
			@NonNull String cpaId,
			@NonNull String conversationId,
			@NonNull String messageId,
			String refToMessageId,
			Instant timeToLive,
			@NonNull String fromPartyId,
			String fromRole,
			@NonNull String toPartyId,
			String toRole,
			@NonNull String service,
			@NonNull String action,
			Document content,
			EbMSMessageStatus status,
			Instant statusTime) throws TransformerException
	{
		this.timestamp = timestamp;
		this.cpaId = cpaId;
		this.conversationId = conversationId;
		this.messageId = messageId;
		this.refToMessageId = refToMessageId;
		this.timeToLive = timeToLive;
		this.fromPartyId = fromPartyId;
		this.fromRole = fromRole;
		this.toPartyId = toPartyId;
		this.toRole = toRole;
		this.service = service;
		this.action = action;
		this.content = DOMUtils.toString(content);
		this.status = status;
		this.statusTime = statusTime;
		this.attachments = Collections.emptyList();
		this.deliveryLogs = Collections.emptyList();
	}
}
