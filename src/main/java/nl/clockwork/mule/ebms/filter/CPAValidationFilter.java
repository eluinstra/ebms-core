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

import java.util.Date;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.StatusValueType;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class CPAValidationFilter implements Filter
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
				Date now = new Date();
				CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(msg.getMessageHeader().getCPAId());
				if (!StatusValueType.AGREED.equals(cpa.getStatus().getValue()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"CPA not agreed."));
					return false;
				}
				if (now.compareTo(cpa.getStart().toGregorianCalendar().getTime()) < 0 || now.compareTo(cpa.getEnd().toGregorianCalendar().getTime()) > 0)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"CPA invalid."));
					return false;
				}

				return StatusValueType.AGREED.equals(cpa.getStatus().getValue())
					&& now.compareTo(cpa.getStart().toGregorianCalendar().getTime()) >= 0
					&& now.compareTo(cpa.getEnd().toGregorianCalendar().getTime()) <= 0
				;
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
