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

import java.util.List;

import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.xmldsig.SignatureType;

public class EbMSMessage implements EbMSBaseMessage
{
	private byte[] original;
	private SignatureType signature;
	private MessageHeader messageHeader;
	private SyncReply syncReply;
	private MessageOrder messageOrder;
	private AckRequested ackRequested;
	private ErrorList errorList;
	private Acknowledgment acknowledgment;
	private Manifest manifest;
	private StatusRequest statusRequest;
	private StatusResponse statusResponse;
	private List<EbMSAttachment> attachments;

	public EbMSMessage(MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(null,null,messageHeader,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(byte[] message, SignatureType signature, MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(message,signature,messageHeader,null,null,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(null,null,messageHeader,syncReply,messageOrder,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(byte[] original, SignatureType signature, MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(original,signature,messageHeader,syncReply,messageOrder,ackRequested,null,null,manifest,null,null,attachments);
	}
	
	public EbMSMessage(MessageHeader messageHeader, ErrorList errorList)
	{
		this(null,null,messageHeader,null,null,null,errorList,null,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, Acknowledgment acknowledgment)
	{
		this(null,null,messageHeader,null,null,null,null,acknowledgment,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, SyncReply syncReply, ErrorList errorList, Acknowledgment acknowledgment)
	{
		this(null,null,messageHeader,syncReply,null,null,errorList,acknowledgment,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, SyncReply syncReply, StatusRequest statusRequest)
	{
		this(null,null,messageHeader,syncReply,null,null,null,null,null,statusRequest,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, StatusResponse statusResponse)
	{
		this(null,null,messageHeader,null,null,null,null,null,null,null,statusResponse,null);
	}
	
	public EbMSMessage(byte[] original, SignatureType signature, MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse, List<EbMSAttachment> attachments)
	{
		this.original = original;
		this.signature = signature;
		this.messageHeader = messageHeader;
		this.syncReply = syncReply;
		this.messageOrder = messageOrder;
		this.ackRequested = ackRequested;
		this.errorList = errorList;
		this.acknowledgment = acknowledgment;
		this.statusRequest = statusRequest;
		this.statusResponse = statusResponse;
		this.manifest = manifest;
		this.attachments = attachments;
	}

	public byte[] getOriginal()
	{
		return original;
	}
	
	public SignatureType getSignature()
	{
		return signature;
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
	
	public MessageOrder getMessageOrder()
	{
		return messageOrder;
	}
	
	public AckRequested getAckRequested()
	{
		return ackRequested;
	}
	
	public ErrorList getErrorList()
	{
		return errorList;
	}
	
	public Acknowledgment getAcknowledgment()
	{
		return acknowledgment;
	}
	
	public Manifest getManifest()
	{
		return manifest;
	}
	
	public StatusRequest getStatusRequest()
	{
		return statusRequest;
	}
	
	public StatusResponse getStatusResponse()
	{
		return statusResponse;
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}
}
