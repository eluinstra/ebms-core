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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.ws.Holder;

import nl.clockwork.common.cxf.AttachmentManager;
import nl.clockwork.mule.ebms.cxf.MessageManager;
import nl.clockwork.mule.ebms.cxf.SignatureManager;
import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSDataSource;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageError;
import nl.clockwork.mule.ebms.model.EbMSMessageStatusRequest;
import nl.clockwork.mule.ebms.model.EbMSMessageStatusResponse;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.MessageOrder;
import nl.clockwork.mule.ebms.model.ebxml.StatusRequest;
import nl.clockwork.mule.ebms.model.ebxml.StatusResponse;
import nl.clockwork.mule.ebms.model.ebxml.SyncReply;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.message.Attachment;

public class EbMSPortTypeImpl implements EbMSPortType
{
  protected transient Log logger = LogFactory.getLog(getClass());
  //private EbMSProcessor processor;
  private EbMSMessageProcessor messageProcessor;
  private EbMSMessageErrorProcessor messageErrorProcessor;
  private EbMSAcknowledgmentProcessor acknowledgmentProcessor;
  private EbMSMessageStatusProcessor messageStatusProcessor;
  private EbMSPingProcessor pingProcessor;

  //@Resource
	//private WebServiceContext context;

	@Override
	public void message(MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Manifest manifest)
	{
		Collection<Attachment> attachments = AttachmentManager.get();
		List<DataSource> dataSources = new ArrayList<DataSource>();
		for (Attachment attachment : attachments)
			dataSources.add(new EbMSDataSource(attachment.getDataHandler().getDataSource(),attachment.getId(),attachment.getDataHandler().getName()));
		messageProcessor.process(new EbMSMessage(MessageManager.get(),SignatureManager.get().getSignature(),messageHeader,syncReply,messageOrder,ackRequested,manifest,dataSources),SignatureManager.get());
	}

	@Override
	public void messageError(MessageHeader messageHeader, ErrorList errorList)
	{
		messageErrorProcessor.process(new EbMSMessageError(messageHeader,errorList));
	}

	@Override
	public void acknowledgment(MessageHeader messageHeader, Acknowledgment acknowledgment)
	{
		acknowledgmentProcessor.process(new EbMSAcknowledgment(messageHeader,acknowledgment));
	}

	@Override
	public void messageStatus(MessageHeader requestMessageHeader, StatusRequest statusRequest, Holder<MessageHeader> responseMessageHeader, Holder<StatusResponse> statusResponse)
	{
		EbMSMessageStatusResponse response = messageStatusProcessor.process(new EbMSMessageStatusRequest(requestMessageHeader,statusRequest));
		responseMessageHeader.value = response.getMessageHeader();
		statusResponse.value = response.getStatusResponse();
	}

	@Override
	public MessageHeader ping(MessageHeader requestMessageHeader)
	{
		return pingProcessor.process(requestMessageHeader);
		//throw new EbMSException("an error occurred!");
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
