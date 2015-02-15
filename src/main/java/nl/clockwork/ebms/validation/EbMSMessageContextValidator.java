/**
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
 */
package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.CPAUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

public class EbMSMessageContextValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	public EbMSMessageContextValidator(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void validate(EbMSMessageContext context) throws ValidatorException
	{
		try
		{
			if (StringUtils.isEmpty(context.getCpaId()))
				throw new ValidationException("context.cpaId cannot be empty!");
			if (StringUtils.isEmpty(context.getService()))
				throw new ValidationException("context.service cannot be empty!");
			if (StringUtils.isEmpty(context.getAction()))
				throw new ValidationException("context.action cannot be empty!");

			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(context.getCpaId());
			if (cpa == null)
				throw new ValidationException("No CPA found for: context.cpaId=" + context.getCpaId());

			FromPartyInfo fromPartyInfo = CPAUtils.getFromPartyInfo(cpa,context.getFromRole(),context.getService(),context.getAction());
			if (fromPartyInfo == null)
			{
				StringBuffer msg = new StringBuffer();
				msg.append("No CanSend action found for:");
				msg.append(" context.cpaId=").append(context.getCpaId());
				if (context.getFromRole() != null)
				{
					msg.append(", context.fromRole.partyId=").append(context.getFromRole().getPartyId());
					msg.append(", context.fromRole.role=").append(context.getFromRole().getRole());
				}
				msg.append(", context.service=").append(context.getService());
				msg.append(", context.action=").append(context.getAction());
				throw new ValidationException(msg.toString());
			}

			//ToPartyInfo otherPartyInfo = CPAUtils.getToPartyInfo(cpa,(ActionBindingType)fromPartyInfo.getCanSend().getOtherPartyActionBinding());
			ToPartyInfo toPartyInfo = CPAUtils.getToPartyInfo(cpa,context.getToRole(),context.getService(),context.getAction());
			//if (otherPartyInfo == null && toPartyInfo == null)
			if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() == null && toPartyInfo == null)
			{
				StringBuffer msg = new StringBuffer();
				msg.append("No CanReceive action found for:");
				msg.append(" context.cpaId=").append(context.getCpaId());
				if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() != null && context.getFromRole() != null)
				{
					msg.append(", context.fromRole.partyId=").append(context.getFromRole().getPartyId());
					msg.append(", context.fromRole.role=").append(context.getFromRole().getRole());
				}
				if (context.getToRole() != null)
				{
					msg.append(", context.toRole.partyId=").append(context.getToRole().getPartyId());
					msg.append(", context.toRole.role=").append(context.getToRole().getRole());
				}
				msg.append(", context.service=").append(context.getService());
				msg.append(", context.action=").append(context.getAction());
				throw new ValidationException(msg.toString());
			}
			//else if (toPartyInfo != null && toPartyInfo1 != null && toPartyInfo.getCanReceive().getThisPartyActionBinding() != toPartyInfo1.getCanReceive().getThisPartyActionBinding())
			else if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() != null && toPartyInfo != null && fromPartyInfo.getCanSend().getOtherPartyActionBinding() != toPartyInfo.getCanReceive().getThisPartyActionBinding())
			{
				StringBuffer msg = new StringBuffer();
				msg.append("Action for to party does not match action for from party for:");
				msg.append(" context.cpaId=").append(context.getCpaId());
				if (fromPartyInfo.getCanSend().getOtherPartyActionBinding() != null && context.getFromRole() != null)
				{
					msg.append(", context.fromRole.partyId=").append(context.getFromRole().getPartyId());
					msg.append(", context.fromRole.role=").append(context.getFromRole().getRole());
				}
				if (context.getToRole() != null)
				{
					msg.append(", context.toRole.partyId=").append(context.getToRole().getPartyId());
					msg.append(", context.toRole.role=").append(context.getToRole().getRole());
				}
				msg.append(", context.service=").append(context.getService());
				msg.append(", context.action=").append(context.getAction());
				msg.append(". Leave context.toRole empty!");
				throw new ValidationException(msg.toString());
			}
		}
		catch (DAOException e)
		{
			throw new ValidatorException(e);
		}
	}

}
