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

import javax.xml.ws.BindingProvider;

import nl.clockwork.mule.common.component.Callable;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.service.EbMS;
import nl.clockwork.mule.ebms.service.EbMSPortType;
import nl.clockwork.mule.ebms.util.CPAUtils;

import org.mule.api.MuleMessage;

public class InvokeEbMSMessage extends Callable
{
	private EbMSDAO ebMSDAO;

	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
		if (message.getPayload() instanceof EbMSMessage)
		{
			EbMS service = new EbMS();
			EbMSPortType ebMS = service.getEbMSPort();

			EbMSMessage msg = (EbMSMessage)message.getPayload();

			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(msg.getMessageHeader().getCPAId());
			CPAUtils.getPartyInfo(cpa,msg.getMessageHeader().getTo().getPartyId());
			//get transportreceiver CPAUtils.
			
			String url = "http://localhost:63081/greeter";
			((BindingProvider)ebMS).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,url);

			ebMS.message(msg.getMessageHeader(),msg.getSyncReply(),msg.getMessageOrder(),msg.getAckRequested(),msg.getManifest());
		}
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
