/**
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
package nl.clockwork.ebms.model;

import java.util.Collections;
import java.util.List;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSMessage extends EbMSRequestMessage
{
	private static final long serialVersionUID = 1L;
	MessageOrder messageOrder;
	AckRequested ackRequested;
	Manifest manifest;
	@NonNull
	List<EbMSAttachment> attachments;

	@Builder
	public EbMSMessage(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		super(messageHeader,signature,syncReply);
		this.messageOrder = messageOrder;
		this.ackRequested = ackRequested;
		this.manifest = manifest;
		this.attachments = attachments == null ? Collections.emptyList() : attachments;
	}

	public String getContentId()
	{
		return getMessageHeader().getMessageData().getMessageId();
	}

}
