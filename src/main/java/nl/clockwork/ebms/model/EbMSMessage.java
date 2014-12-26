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
import org.w3._2000._09.xmldsig.SignatureType;
import org.w3c.dom.Document;

public class EbMSMessage extends EbMSDocument
{
	private Document message;
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

	@Override
	public Document getMessage()
	{
		return message;
	}
	
	public void setMessage(Document document)
	{
		this.message = document;
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
	
	public void setMessageHeader(MessageHeader messageHeader)
	{
		this.messageHeader = messageHeader;
	}
	
	public SyncReply getSyncReply()
	{
		return syncReply;
	}
	
	public void setSyncReply(SyncReply syncReply)
	{
		this.syncReply = syncReply;
	}
	
	public MessageOrder getMessageOrder()
	{
		return messageOrder;
	}
	
	public void setMessageOrder(MessageOrder messageOrder)
	{
		this.messageOrder = messageOrder;
	}
	
	public AckRequested getAckRequested()
	{
		return ackRequested;
	}
	
	public void setAckRequested(AckRequested ackRequested)
	{
		this.ackRequested = ackRequested;
	}
	
	public ErrorList getErrorList()
	{
		return errorList;
	}
	
	public void setErrorList(ErrorList errorList)
	{
		this.errorList = errorList;
	}
	
	public Acknowledgment getAcknowledgment()
	{
		return acknowledgment;
	}
	
	public void setAcknowledgment(Acknowledgment acknowledgment)
	{
		this.acknowledgment = acknowledgment;
	}
	
	public Manifest getManifest()
	{
		return manifest;
	}
	
	public void setManifest(Manifest manifest)
	{
		this.manifest = manifest;
	}
	
	public StatusRequest getStatusRequest()
	{
		return statusRequest;
	}
	
	public void setStatusRequest(StatusRequest statusRequest)
	{
		this.statusRequest = statusRequest;
	}
	
	public StatusResponse getStatusResponse()
	{
		return statusResponse;
	}
	
	public void setStatusResponse(StatusResponse statusResponse)
	{
		this.statusResponse = statusResponse;
	}

	@Override
	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}
	
	public void setAttachments(List<EbMSAttachment> attachments)
	{
		this.attachments = attachments;
	}
}
