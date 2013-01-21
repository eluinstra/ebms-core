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
package nl.clockwork.mule.ebms.service;

import javax.xml.ws.Holder;

import nl.clockwork.ebms.AttachmentManager;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
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
import nl.clockwork.ebms.service.EbMSPortType;
import nl.clockwork.mule.ebms.cxf.MessageManager;
import nl.clockwork.mule.ebms.cxf.SignatureManager;
import nl.clockwork.mule.ebms.processor.EbMSAcknowledgmentProcessor;
import nl.clockwork.mule.ebms.processor.EbMSMessageErrorProcessor;
import nl.clockwork.mule.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.mule.ebms.processor.EbMSMessageStatusProcessor;
import nl.clockwork.mule.ebms.processor.EbMSPingProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSPortTypeImpl implements EbMSPortType
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSMessageProcessor messageProcessor;
  private EbMSMessageErrorProcessor messageErrorProcessor;
  private EbMSAcknowledgmentProcessor acknowledgmentProcessor;
  private EbMSMessageStatusProcessor messageStatusProcessor;
  private EbMSPingProcessor pingProcessor;

  //@Resource
	//private WebServiceContext context;

	@Override
	public void message(MessageHeader messageHeader, MessageOrder messageOrder, AckRequested ackRequested, ErrorList errorList, Acknowledgment acknowledgment, Manifest manifest, StatusRequest statusRequest, StatusResponse statusResponse)
	{
		if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
			messageProcessor.process(new EbMSMessage(MessageManager.get(),SignatureManager.get() == null ? null : SignatureManager.get(),messageHeader,null,messageOrder,ackRequested,manifest,AttachmentManager.get()));
		else if (EbMSMessageType.MESSAGE_ERROR.action().equals(messageHeader.getAction()))
			messageErrorProcessor.process(new EbMSMessageError(messageHeader,errorList));
		else if (EbMSMessageType.ACKNOWLEDGMENT.action().equals(messageHeader.getAction()))
			acknowledgmentProcessor.process(new EbMSAcknowledgment(messageHeader,acknowledgment));
		else if (EbMSMessageType.STATUS_REQUEST.action().equals(messageHeader.getAction()))
		{
			EbMSStatusResponse response = messageStatusProcessor.process(new EbMSStatusRequest(messageHeader,null,statusRequest));
			if (response != null)
			{
				//responseMessageHeader.value = response.getMessageHeader();
				//statusResponse.value = response.getStatusResponse();
			}
		}
		else if (EbMSMessageType.STATUS_RESPONSE.action().equals(messageHeader.getAction()))
		{
			
		}
		else if (EbMSMessageType.PING.action().equals(messageHeader.getAction()))
		{
			//FIXME check for NullPayload and return null??, response has to be the same as without fix (so empty)
			//EbMSPong result = pingProcessor.process(new EbMSPing(messageHeader,null));
			//return result.getMessageHeader();
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
			EbMSStatusResponse response = messageStatusProcessor.process(new EbMSStatusRequest(requestMessageHeader,syncReply,statusRequest));
			if (response != null)
			{
				responseMessageHeader.value = response.getMessageHeader();
				statusResponse.value = response.getStatusResponse();
			}
		}
		else if (EbMSMessageType.PING.action().equals(requestMessageHeader.getAction()))
		{
			EbMSPong response = pingProcessor.process(new EbMSPing(requestMessageHeader,null));
			responseMessageHeader.value = response.getMessageHeader();
		}
	}

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
	
	public void setMessageErrorProcessor(EbMSMessageErrorProcessor messageErrorProcessor)
	{
		this.messageErrorProcessor = messageErrorProcessor;
	}
	
	public void setAcknowledgmentProcessor(EbMSAcknowledgmentProcessor acknowledgmentProcessor)
	{
		this.acknowledgmentProcessor = acknowledgmentProcessor;
	}
	
	public void setMessageStatusProcessor(EbMSMessageStatusProcessor messageStatusProcessor)
	{
		this.messageStatusProcessor = messageStatusProcessor;
	}
	
	public void setPingProcessor(EbMSPingProcessor pingProcessor)
	{
		this.pingProcessor = pingProcessor;
	}

}
