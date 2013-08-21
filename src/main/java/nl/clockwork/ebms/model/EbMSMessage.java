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

import java.util.ArrayList;
import java.util.List;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig_.SignatureType;

public class EbMSMessage
{
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
	private List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();

	public EbMSMessage(MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(null,messageHeader,null,null,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(signature,messageHeader,null,null,ackRequested,manifest,attachments);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, List<EbMSAttachment> attachments)
	{
		this(signature,messageHeader,syncReply,messageOrder,ackRequested,null,null,manifest,null,null,attachments);
	}
	
	public EbMSMessage(MessageHeader messageHeader)
	{
		this(null,messageHeader,null,null,null,null,null,null,null,null,null);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader)
	{
		this(signature,messageHeader,null,null,null,null,null,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, ErrorList errorList)
	{
		this(null,messageHeader,null,null,null,errorList,null,null,null,null,null);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, ErrorList errorList)
	{
		this(signature,messageHeader,null,null,null,errorList,null,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, Acknowledgment acknowledgment)
	{
		this(null,messageHeader,null,null,null,null,acknowledgment,null,null,null,null);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, Acknowledgment acknowledgment)
	{
		this(signature,messageHeader,null,null,null,null,acknowledgment,null,null,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, SyncReply syncReply, ErrorList errorList, Acknowledgment acknowledgment)
	{
		this(messageHeader,syncReply,null,null,errorList,acknowledgment,null,null,null,null);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, SyncReply syncReply, StatusRequest statusRequest)
	{
		this(signature,messageHeader,syncReply,null,null,null,null,null,statusRequest,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, StatusRequest statusRequest)
	{
		this(null,messageHeader,null,null,null,null,null,null,statusRequest,null,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, StatusResponse statusResponse)
	{
		this(null,messageHeader,null,null,null,null,null,null,null,statusResponse,null);
	}
	
	public EbMSMessage(SignatureType signature, MessageHeader messageHeader, StatusResponse statusResponse)
	{
		this(signature,messageHeader,null,null,null,null,null,null,null,statusResponse,null);
	}
	
	public EbMSMessage(MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse, List<EbMSAttachment> attachments)
	{
		this(null,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}
	
	public EbMSMessage(SignatureType signature,MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse, List<EbMSAttachment> attachments)
	{
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
		this.attachments = attachments == null ? new ArrayList<EbMSAttachment>() : attachments;
	}

	public SignatureType getSignature()
	{
		return signature;
	}
	
	public void setSignature(SignatureType signature)
	{
		this.signature = signature;
	}
	
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
