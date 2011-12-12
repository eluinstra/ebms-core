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
package nl.clockwork.mule.ebms.filter;

import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.Signature;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.util.CPAUtils;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSSignatureValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof EbMSBaseMessage)
		{
			try
			{
				EbMSBaseMessage msg = (EbMSBaseMessage)message.getPayload();
				MessageHeader messageHeader = msg.getMessageHeader();
				CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
				Signature signature = (Signature)message.getProperty(Constants.EBMS_SIGNATURE);
				PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
				List<DeliveryChannel> deliveryChannels = CPAUtils.getDeliveryChannels(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
				if (CPAUtils.isSigned(deliveryChannels.get(0)))
				{
					if (signature == null)
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"No signature found."));
						return false;
					}
					if (!signature.isValid())
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Signature invalid."));
						return false;
					}
				}
				return true;
			}
			catch (DAOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return true;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
