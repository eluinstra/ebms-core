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
package nl.clockwork.ebms.service;

import javax.xml.ws.Holder;

import nl.clockwork.ebms.AttachmentManager;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.mule.ebms.cxf.MessageManager;
import nl.clockwork.mule.ebms.cxf.SignatureManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSPortTypeImpl implements EbMSPortType
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSMessageProcessor messageProcessor;

	@Override
	public void message(MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest)
	{
		messageProcessor.process(new EbMSMessage(MessageManager.get(),SignatureManager.get(),messageHeader,syncReply,messageOrder,ackRequested,manifest,AttachmentManager.get()));
	}

	@Override
	public void messageError(MessageHeader messageHeader, ErrorList errorList)
	{
		messageProcessor.process(new EbMSMessageError(messageHeader,errorList));
	}

	@Override
	public void acknowledgment(MessageHeader messageHeader, Acknowledgment acknowledgment)
	{
		messageProcessor.process(new EbMSAcknowledgment(messageHeader,acknowledgment));
	}

	@Override
	public void messageStatus(MessageHeader requestMessageHeader, SyncReply syncReply, StatusRequest statusRequest, Holder<MessageHeader> responseMessageHeader, Holder<StatusResponse> statusResponse)
	{
		EbMSBaseMessage response = messageProcessor.process(new EbMSStatusRequest(requestMessageHeader,syncReply,statusRequest));
		if (response != null)
		{
			if (response instanceof EbMSStatusResponse)
			{
				responseMessageHeader.value = response.getMessageHeader();
				statusResponse.value = ((EbMSStatusResponse)response).getStatusResponse();
			}
		}
	}

	@Override
	public MessageHeader ping(MessageHeader messageHeader, SyncReply syncReply)
	{
		//FIXME check for NullPayload and return null??, response has to be the same as without fix (so empty)
		try
		{
			EbMSBaseMessage result = messageProcessor.process(new EbMSPing(messageHeader,syncReply));
			if (result instanceof EbMSPong)
				return result.getMessageHeader();
			else
				return null;
		}
		catch (ClassCastException e)
		{
			return null;
		}
	}

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
	
}
