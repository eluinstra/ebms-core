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

import nl.clockwork.common.cxf.AttachmentManager;
import nl.clockwork.mule.ebms.cxf.MessageManager;
import nl.clockwork.mule.ebms.cxf.SignatureManager;
import nl.clockwork.mule.ebms.model.EbMSDataSource;
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
  private EbMSMessageProcessor messageProcessor;

  //@Resource
	//private WebServiceContext context;

  @Override
  public void message(MessageHeader messageHeader, SyncReply syncReply, MessageOrder messageOrder, AckRequested ackRequested, Acknowledgment acknowledgment, ErrorList errorList, StatusRequest statusRequest, StatusResponse statusResponse, Manifest manifest)
  {
		//Collection<Attachment> attachments = (Collection<Attachment>)context.getMessageContext().get(SoapMessage.ATTACHMENTS);
		Collection<Attachment> attachments = AttachmentManager.get();
		List<DataSource> dataSources = new ArrayList<DataSource>();
		for (Attachment attachment : attachments)
			dataSources.add(new EbMSDataSource(attachment.getDataHandler().getDataSource(),attachment.getId(),attachment.getDataHandler().getName()));
		messageProcessor.process(MessageManager.get(),messageHeader,syncReply,messageOrder,ackRequested,acknowledgment,errorList,statusRequest,statusResponse,manifest,dataSources,SignatureManager.get());
  }
  
	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}
	
}
