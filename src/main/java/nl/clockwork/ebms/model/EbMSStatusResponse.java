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
import nl.clockwork.ebms.model.ebxml.StatusResponse;

public class EbMSStatusResponse implements EbMSBaseMessage
{
	private MessageHeader messageHeader;
	private StatusResponse statusResponse;

	public EbMSStatusResponse(MessageHeader messageHeader, StatusResponse statusResponse)
	{
		this.messageHeader = messageHeader;
		this.statusResponse = statusResponse;
	}

	@Override
	public MessageHeader getMessageHeader()
	{
		return messageHeader;
	}

	public StatusResponse getStatusResponse()
	{
		return statusResponse;
	}
}
