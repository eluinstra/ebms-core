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
package nl.clockwork.mule.ebms.model;

import nl.clockwork.mule.ebms.Constants.EbMSAcknowledgmentType;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class Acknowledgment
{
	private EbMSAcknowledgmentType acknowledgmentType;
	private MessageHeader messageHeader;
	private Object acknowledgment;

	public Acknowledgment(EbMSAcknowledgmentType acknowledgmentType, MessageHeader messageHeader, Object acknowledgment)
	{
		this.acknowledgmentType = acknowledgmentType;
		this.messageHeader = messageHeader;
		this.acknowledgment = acknowledgment;
	}

	public EbMSAcknowledgmentType getAcknowledgmentType()
	{
		return acknowledgmentType;
	}

	public MessageHeader getMessageHeader()
	{
		return messageHeader;
	}
	
	public Object getAcknowledgment()
	{
		return acknowledgment;
	}
}
