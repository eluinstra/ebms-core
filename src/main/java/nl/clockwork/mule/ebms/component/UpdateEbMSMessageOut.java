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

import java.util.Date;

import javax.xml.datatype.Duration;

import nl.clockwork.mule.common.component.Callable;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;

import org.mule.api.MuleMessage;

public class UpdateEbMSMessageOut extends Callable
{
	private EbMSDAO ebMSDAO;

	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
		if (message.getPayload() instanceof Object[])
		{
			Object[] msg = (Object[])message.getPayload();

			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(((MessageHeader)msg[0]).getCPAId());
			Duration d = CPAUtils.getDuration(cpa,((MessageHeader)msg[0]));
			Date nextRetryTime = new Date();
			d.addTo(nextRetryTime);

			ebMSDAO.updateMessage(message.getLongProperty(Constants.EBMS_MESSAGE_ID,0),nextRetryTime);
		}
		return message;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
