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
package nl.clockwork.mule.ebms.component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.datatype.Duration;

import nl.clockwork.common.cxf.AttachmentManager;
import nl.clockwork.mule.common.component.Callable;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.Constants.EbMSMessageType;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;

import org.apache.cxf.message.Attachment;
import org.mule.api.MuleMessage;

public class InsertEbMSMessageOut extends Callable
{
	private EbMSDAO ebMSDAO;

	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
		if (message.getPayload() instanceof Object[])
		{
			Object[] msg = (Object[])message.getPayload();
			List<DataSource> attachments = new ArrayList<DataSource>();
			for (Attachment a : AttachmentManager.get())
				attachments.add(a.getDataHandler().getDataSource());
			Date date = new Date();

			Date nextRetryTime = null;
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(((MessageHeader)msg[0]).getCPAId());
			Duration d = CPAUtils.getDuration(cpa,((MessageHeader)msg[0]));
			if (d != null)
			{
				nextRetryTime = new Date();
				d.addTo(nextRetryTime);
			}

			long id = ebMSDAO.insertMessage(date,((MessageHeader)msg[0]).getCPAId(),((MessageHeader)msg[0]).getConversationId(),((MessageHeader)msg[0]).getMessageData().getMessageId(),EbMSMessageType.OUT,new byte[]{},(MessageHeader)msg[0],(AckRequested)msg[1],(Manifest)msg[2],EbMSMessageStatus.STORED,attachments,nextRetryTime);
			message.setProperty(Constants.EBMS_MESSAGE_ID,id);
		}
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
