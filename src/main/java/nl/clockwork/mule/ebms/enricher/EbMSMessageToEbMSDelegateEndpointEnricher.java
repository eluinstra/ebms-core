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
package nl.clockwork.mule.ebms.enricher;

import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.Channel;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.cpp.cpa.CanReceive;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;

public class EbMSMessageToEbMSDelegateEndpointEnricher extends AbstractMessageAwareTransformer
{
	private EbMSDAO ebMSDAO;

	public EbMSMessageToEbMSDelegateEndpointEnricher()
	{
		//registerSourceType(EbMSMessage.class);
	}

	@Override
	public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			EbMSMessage msg = (EbMSMessage)message.getPayload();
			MessageHeader messageHeader = msg.getMessageHeader();

			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getTo().getPartyId());
			CanReceive canReceive = CPAUtils.getCanReceive(partyInfo,messageHeader.getTo().getRole(),messageHeader.getService(),messageHeader.getAction());
			Channel channel = ebMSDAO.getChannel(messageHeader.getCPAId(),canReceive.getThisPartyActionBinding().getId());
			if (channel == null)
				throw new Exception("No channel found for cpaId " + messageHeader.getCPAId() + " and actionId " + canReceive.getThisPartyActionBinding().getId());
			message.setProperty(Constants.EBMS_DELEGATE_PATH,channel.getEndpoint());
			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
}
