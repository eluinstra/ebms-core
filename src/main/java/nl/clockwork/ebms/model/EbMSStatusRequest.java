/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.model;

import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.SyncReply;

public class EbMSStatusRequest implements EbMSBaseMessage
{
	private MessageHeader messageHeader;
	private SyncReply syncReply;
	private StatusRequest statusRequest;

	public EbMSStatusRequest(MessageHeader messageHeader, SyncReply syncReply, StatusRequest statusRequest)
	{
		this.messageHeader = messageHeader;
		this.syncReply = syncReply;
		this.statusRequest = statusRequest;
	}

	@Override
	public MessageHeader getMessageHeader()
	{
		return messageHeader;
	}

	public SyncReply getSyncReply()
	{
		return syncReply;
	}

	public StatusRequest getStatusRequest()
	{
		return statusRequest;
	}
}
