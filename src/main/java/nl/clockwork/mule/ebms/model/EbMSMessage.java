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

import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class EbMSMessage implements EbMSBaseMessage
{
	private byte[] message;
	private MessageHeader messageHeader;
	private AckRequested ackRequested;
	private Manifest manifest;
	private List<DataSource> attachments;

	public EbMSMessage(MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<DataSource> attachments)
	{
		this(null,messageHeader,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(byte[] message, MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<DataSource> attachments)
	{
		this.message = message;
		this.messageHeader = messageHeader;
		this.ackRequested = ackRequested;
		this.manifest = manifest;
		this.attachments = attachments;
	}
	
	public byte[] getMessage()
	{
		return message;
	}
	
	@Override
	public MessageHeader getMessageHeader()
	{
		return messageHeader;
	}
	
	public AckRequested getAckRequested()
	{
		return ackRequested;
	}

	public Manifest getManifest()
	{
		return manifest;
	}
	
	public List<DataSource> getAttachments()
	{
		return attachments;
	}
}
