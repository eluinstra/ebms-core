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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public abstract class EbMSRequestMessage extends EbMSBaseMessage
{
	private static final long serialVersionUID = 1L;
	SyncReply syncReply;

	public EbMSRequestMessage(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply)
	{
		super(messageHeader,signature);
		this.syncReply = syncReply;
	}

	public boolean isSyncReply(CPAManager cpaManager)
	{
		try
		{
			//return message.getSyncReply() != null;
			val messageHeader = this.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			val syncReply = cpaManager.getSyncReply(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SyncReply",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (Exception e)
		{
			return this.getSyncReply() != null;
		}
	}

}
