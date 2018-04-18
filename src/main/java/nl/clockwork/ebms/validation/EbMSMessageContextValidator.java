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

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;

import org.apache.commons.lang.StringUtils;

public class EbMSMessageContextValidator
{
	protected CPAManager cpaManager;

	public EbMSMessageContextValidator(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void validate(String cpaId, Party fromParty, Party toParty) throws ValidationException
	{
		if (StringUtils.isEmpty(cpaId))
			throw new ValidationException("cpaId cannot be empty!");
		if (!cpaManager.existsCPA(cpaId))
			throw new ValidationException("No CPA found for cpaId=" + cpaId);
		if (!cpaManager.existsParty(cpaId,fromParty))
		{
			StringBuffer msg = new StringBuffer();
			msg.append("No fromParty found for:");
			msg.append(" context.cpaId=").append(cpaId);
			msg.append(", context.fromParty.partyId=").append(fromParty.getPartyId());
			msg.append(", context.fromParty.role=").append(fromParty.getRole());
			throw new ValidationException(msg.toString());
		}
		if (!cpaManager.existsParty(cpaId,toParty))
		{
			StringBuffer msg = new StringBuffer();
			msg.append("No toParty found for:");
			msg.append(" context.cpaId=").append(cpaId);
			msg.append(", context.toParty.partyId=").append(toParty.getPartyId());
			msg.append(", context.toParty.role=").append(toParty.getRole());
			throw new ValidationException(msg.toString());
		}
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

			if (!cpaManager.existsCPA(context.getCpaId()))
				throw new ValidationException("No CPA found for: context.cpaId=" + context.getCpaId());

			FromPartyInfo fromPartyInfo = cpaManager.getFromPartyInfo(context.getCpaId(),context.getFromRole(),context.getService(),context.getAction());
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
			ToPartyInfo toPartyInfo = cpaManager.getToPartyInfo(context.getCpaId(),context.getToRole(),context.getService(),context.getAction());
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
