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
package nl.clockwork.ebms.service.a;

import javax.xml.ws.Holder;

import nl.clockwork.ebms.AttachmentManager;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSPing;
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
	public void message(MessageHeader messageHeader, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse)
	{
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
			messageProcessor.process(new EbMSMessage(MessageManager.get(),SignatureManager.get(),messageHeader,null,messageOrder,ackRequested,manifest,AttachmentManager.get()));
		else if (EbMSMessageType.MESSAGE_ERROR.action().equals(messageHeader.getAction()))
			messageProcessor.process(new EbMSMessageError(messageHeader,errorList));
		else if (EbMSMessageType.ACKNOWLEDGMENT.action().equals(messageHeader.getAction()))
			messageProcessor.process(new EbMSAcknowledgment(messageHeader,acknowledgment));
		else if (EbMSMessageType.STATUS_REQUEST.action().equals(messageHeader.getAction()))
		{
			EbMSBaseMessage response = messageProcessor.process(new EbMSStatusRequest(messageHeader,null,statusRequest));
			if (response != null)
			{
				if (response instanceof EbMSStatusResponse)
				{
					//messageHeader.value = response.getMessageHeader();
					//statusResponse.value = ((EbMSStatusResponse)response).getStatusResponse();
				}
			}
		}
		else if (EbMSMessageType.STATUS_RESPONSE.action().equals(messageHeader.getAction()))
		{
			
		}
		else if (EbMSMessageType.PING.action().equals(messageHeader.getAction()))
		{
			//EbMSBaseMessage result = messageProcessor.process(new EbMSPing(messageHeader,null));
			//if (result instanceof EbMSPong)
				//return result.getMessageHeader();
			//else
				//return null;
		}
		else if (EbMSMessageType.PONG.action().equals(messageHeader.getAction()))
		{
			
		}
	}
	
	@Override
	public void syncMessage(MessageHeader requestMessageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest, StatusRequest statusRequest, Holder<MessageHeader> responseMessageHeader, Holder<ErrorList> errorList, Holder<Acknowledgment> acknowledgment, Holder<StatusResponse> statusResponse)
	{
		if (!Constants.EBMS_SERVICE_URI.equals(requestMessageHeader.getService().getValue()))
			messageProcessor.process(new EbMSMessage(MessageManager.get(),SignatureManager.get() == null ? null : SignatureManager.get(),requestMessageHeader,syncReply,messageOrder,ackRequested,manifest,AttachmentManager.get()));
		else if (EbMSMessageType.STATUS_REQUEST.action().equals(requestMessageHeader.getAction()))
		{
			EbMSBaseMessage response = messageProcessor.process(new EbMSStatusRequest(requestMessageHeader,syncReply,statusRequest));
			if (response != null)
			{
				responseMessageHeader.value = response.getMessageHeader();
				if (response instanceof EbMSStatusResponse)
				{
					statusResponse.value = ((EbMSStatusResponse)response).getStatusResponse();
				}
			}
		}
		else if (EbMSMessageType.PING.action().equals(requestMessageHeader.getAction()))
		{
			EbMSBaseMessage response = messageProcessor.process(new EbMSPing(requestMessageHeader,syncReply));
			responseMessageHeader.value = response.getMessageHeader();
			if (response instanceof EbMSMessageError)
				errorList.value = ((EbMSMessageError)response).getErrorList();
		}
	}

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
	
}
