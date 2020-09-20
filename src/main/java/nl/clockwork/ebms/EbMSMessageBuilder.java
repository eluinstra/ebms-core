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
package nl.clockwork.ebms;

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

import static io.vavr.API.*;
import lombok.NonNull;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.processor.EbMSProcessingException;

public class EbMSMessageBuilder
{
	private MessageHeader messageHeader;
	private SyncReply syncReply;
	private MessageOrder messageOrder;
	private AckRequested ackRequested;
	private ErrorList errorList;
	private Acknowledgment acknowledgment;
	private Manifest manifest;
	private StatusRequest statusRequest;
	private StatusResponse statusResponse;
	private SignatureType signature;
	private boolean attachments$set;
	private List<EbMSAttachment> attachments$value;

	public EbMSMessageBuilder()
	{
	}

	public EbMSMessageBuilder messageHeader(@NonNull final MessageHeader messageHeader)
	{
		this.messageHeader = messageHeader;
		return this;
	}

	public EbMSMessageBuilder syncReply(final SyncReply syncReply)
	{
		this.syncReply = syncReply;
		return this;
	}

	public EbMSMessageBuilder messageOrder(final MessageOrder messageOrder)
	{
		this.messageOrder = messageOrder;
		return this;
	}

	public EbMSMessageBuilder ackRequested(final AckRequested ackRequested)
	{
		this.ackRequested = ackRequested;
		return this;
	}

	public EbMSMessageBuilder errorList(final ErrorList errorList)
	{
		this.errorList = errorList;
		return this;
	}

	public EbMSMessageBuilder acknowledgment(final Acknowledgment acknowledgment)
	{
		this.acknowledgment = acknowledgment;
		return this;
	}

	public EbMSMessageBuilder manifest(final Manifest manifest)
	{
		this.manifest = manifest;
		return this;
	}

	public EbMSMessageBuilder statusRequest(final StatusRequest statusRequest)
	{
		this.statusRequest = statusRequest;
		return this;
	}

	public EbMSMessageBuilder statusResponse(final StatusResponse statusResponse)
	{
		this.statusResponse = statusResponse;
		return this;
	}

	public EbMSMessageBuilder signature(final SignatureType signature)
	{
		this.signature = signature;
		return this;
	}

	public EbMSMessageBuilder attachments(@NonNull final List<EbMSAttachment> attachments)
	{
		this.attachments$value = attachments;
		attachments$set = true;
		return this;
	}

	public EbMSBaseMessage build()
	{
		try
		{
			List<EbMSAttachment> attachments$value = this.attachments$value;
			if (!this.attachments$set) attachments$value = new ArrayList<EbMSAttachment>();
			if (!EbMSAction.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
				return EbMSMessage.builder()
						.messageHeader(messageHeader)
						.signature(signature)
						.syncReply(syncReply)
						.messageOrder(messageOrder)
						.ackRequested(ackRequested)
						.manifest(manifest)
						.attachments(attachments$value)
						.build();
			else
				return Match(messageHeader.getAction()).of(
						Case($(EbMSAction.MESSAGE_ERROR.getAction()),o -> new EbMSMessageError(messageHeader,signature,errorList)),
						Case($(EbMSAction.ACKNOWLEDGMENT.getAction()),o -> new EbMSAcknowledgment(messageHeader,signature,acknowledgment)),
						Case($(EbMSAction.STATUS_REQUEST.getAction()),o -> new EbMSStatusRequest(messageHeader,signature,syncReply,statusRequest)),
						Case($(EbMSAction.STATUS_RESPONSE.getAction()),o -> new EbMSStatusResponse(messageHeader,signature,statusResponse)),
						Case($(EbMSAction.PING.getAction()),o -> new EbMSPing(messageHeader,signature,syncReply)),
						Case($(EbMSAction.PONG.getAction()),o -> new EbMSPong(messageHeader,signature)),
						Case($(),o -> {
							throw new EbMSProcessingException("Unable to build message from service " + CPAUtils.toString(messageHeader.getService()) + " and action " + messageHeader.getAction());
						}));
		}
		catch (EbMSProcessingException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new EbMSProcessingException("Unable to build a valid message message!",e);
		}
	}

	@java.lang.Override
	public java.lang.String toString()
	{
		return "EbMSMessage.EbMSMessageBuilder(messageHeader=" + this.messageHeader + ", syncReply=" + this.syncReply + ", messageOrder=" + this.messageOrder + ", ackRequested=" + this.ackRequested + ", errorList=" + this.errorList + ", acknowledgment=" + this.acknowledgment + ", manifest=" + this.manifest + ", statusRequest=" + this.statusRequest + ", statusResponse=" + this.statusResponse + ", signature=" + this.signature + ", attachments$value=" + this.attachments$value + ")";
	}
}