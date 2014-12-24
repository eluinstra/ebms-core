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
package nl.clockwork.ebms.util;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageContext;

import org.apache.commons.lang.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;

public class EbMSMessageContextValidator
{
	private EbMSDAO ebMSDAO;
	
	public EbMSMessageContextValidator(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void validate(EbMSMessageContext context)
	{
		try
		{
			if (StringUtils.isEmpty(context.getCpaId()))
				throw new EbMSMessageContextValidationException("context.cpaId cannot be empty!");
			if (StringUtils.isEmpty(context.getService()))
				throw new EbMSMessageContextValidationException("context.service cannot be empty!");
			if (StringUtils.isEmpty(context.getAction()))
				throw new EbMSMessageContextValidationException("context.action cannot be empty!");

			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(context.getCpaId());
			if (cpa == null)
				throw new EbMSMessageContextValidationException("No CPA found for: context.cpaId=" + context.getCpaId());

			PartyInfo sendingPartyInfo = CPAUtils.getSendingPartyInfo(cpa,context.getFromRole(),context.getService(),context.getAction());
			if (sendingPartyInfo == null)
			{
				StringBuffer msg = new StringBuffer();
				msg.append("No CanSend action found for:");
				msg.append(" context.cpaId=").append(context.getCpaId());
				if (context.getFromRole() != null)
					msg.append(", context.fromRole=").append(context.getFromRole());
				msg.append(", context.service=").append(context.getService());
				msg.append(", context.action=").append(context.getAction());
				throw new EbMSMessageContextValidationException(msg.toString());
			}

			//PartyInfo receivingPartyInfo = CPAUtils.getReceivingPartyInfo(cpa,(ActionBindingType)sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getOtherPartyActionBinding());
			PartyInfo receivingPartyInfo1 = CPAUtils.getReceivingPartyInfo(cpa,context.getToRole(),context.getService(),context.getAction());
			//if (receivingPartyInfo == null && receivingPartyInfo1 == null)
			if (sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getOtherPartyActionBinding() == null && receivingPartyInfo1 == null)
			{
				StringBuffer msg = new StringBuffer();
				msg.append("No CanReceive action found for:");
				msg.append(" context.cpaId=").append(context.getCpaId());
				if (sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getOtherPartyActionBinding() != null && context.getFromRole() != null)
					msg.append(", context.fromRole=").append(context.getFromRole());
				if (context.getToRole() != null)
					msg.append(", context.toRole=").append(context.getToRole());
				msg.append(", context.service=").append(context.getService());
				msg.append(", context.action=").append(context.getAction());
				throw new EbMSMessageContextValidationException(msg.toString());
			}
			//else if (receivingPartyInfo != null && receivingPartyInfo1 != null && receivingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanReceive().get(0).getThisPartyActionBinding() != receivingPartyInfo1.getCollaborationRole().get(0).getServiceBinding().getCanReceive().get(0).getThisPartyActionBinding())
			else if (sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getOtherPartyActionBinding() != null && receivingPartyInfo1 != null && sendingPartyInfo.getCollaborationRole().get(0).getServiceBinding().getCanSend().get(0).getOtherPartyActionBinding() != receivingPartyInfo1.getCollaborationRole().get(0).getServiceBinding().getCanReceive().get(0).getThisPartyActionBinding())
				throw new EbMSMessageContextValidationException("to party does not match from party for this action. Leave context.toRole empty!");
		}
		catch (DAOException e)
		{
			throw new EbMSMessageContextValidatorException(e);
		}
	}
	
}
