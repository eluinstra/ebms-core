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

import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.Party;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageContextValidator
{
	@NonNull
	CPAManager cpaManager;

	public void validate(String cpaId, String fromPartyId, String toPartyId) throws ValidationException
	{
		if (StringUtils.isEmpty(cpaId))
			throw new ValidationException("cpaId cannot be empty!");
		if (!cpaManager.existsCPA(cpaId))
			throw new ValidationException("No CPA found for cpaId=" + cpaId);
		if (!cpaManager.existsPartyId(cpaId,fromPartyId))
		{
			val msg = new StringBuffer();
			msg.append("No fromParty found for:");
			msg.append(" cpaId=").append(cpaId);
			msg.append(", fromPartyId=").append(fromPartyId);
			throw new ValidationException(msg.toString());
		}
		if (!cpaManager.existsPartyId(cpaId,toPartyId))
		{
			val msg = new StringBuffer();
			msg.append("No toParty found for:");
			msg.append(" cpaId=").append(cpaId);
			msg.append(", toPartyId=").append(toPartyId);
			throw new ValidationException(msg.toString());
		}
	}

	public void validate(EbMSMessageContext context) throws ValidatorException
	{
		if (StringUtils.isEmpty(context.getCpaId()))
			throw new ValidationException("context.cpaId cannot be empty!");
		if (isEmpty(context.getFromParty()))
			throw new ValidationException("context.fromParty cannot be empty!");
		if (StringUtils.isEmpty(context.getService()))
			throw new ValidationException("context.service cannot be empty!");
		if (StringUtils.isEmpty(context.getAction()))
			throw new ValidationException("context.action cannot be empty!");

		if (!cpaManager.existsCPA(context.getCpaId()))
			throw new ValidationException("No CPA found for: context.cpaId=" + context.getCpaId());

		val fromPartyInfo =
				cpaManager.getFromPartyInfo(context.getCpaId(),context.getFromParty(),context.getService(),context.getAction());
		if (!fromPartyInfo.isPresent())
		{
			val msg = new StringBuffer();
			msg.append("No CanSend action found for:");
			msg.append(" context.cpaId=").append(context.getCpaId());
			if (context.getFromParty() != null)
			{
				msg.append(", context.FromParty.partyId=").append(context.getFromParty().getPartyId());
				msg.append(", context.FromParty.role=").append(context.getFromParty().getRole());
			}
			msg.append(", context.service=").append(context.getService());
			msg.append(", context.action=").append(context.getAction());
			throw new ValidationException(msg.toString());
		}

		val toPartyInfo =
				cpaManager.getToPartyInfo(context.getCpaId(),context.getToParty(),context.getService(),context.getAction());
		if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() == null && !toPartyInfo.isPresent())
		{
			val msg = new StringBuffer();
			msg.append("No CanReceive action found for:");
			msg.append(" context.cpaId=").append(context.getCpaId());
			if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() != null && context.getFromParty() != null)
			{
				msg.append(", context.FromParty.partyId=").append(context.getFromParty().getPartyId());
				msg.append(", context.FromParty.role=").append(context.getFromParty().getRole());
			}
			if (context.getToParty() != null)
			{
				msg.append(", context.ToParty.partyId=").append(context.getToParty().getPartyId());
				msg.append(", context.ToParty.role=").append(context.getToParty().getRole());
			}
			msg.append(", context.service=").append(context.getService());
			msg.append(", context.action=").append(context.getAction());
			throw new ValidationException(msg.toString());
		}
		else if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() != null
				&& toPartyInfo.isPresent()
				&& !equals(toPartyInfo.get().getCanReceive().getThisPartyActionBinding(),fromPartyInfo.get().getCanSend().getOtherPartyActionBinding()))
		{
			val msg = new StringBuffer();
			msg.append("Action for to party does not match action for from party for:");
			msg.append(" context.cpaId=").append(context.getCpaId());
			if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() != null && context.getFromParty() != null)
			{
				msg.append(", context.FromParty.partyId=").append(context.getFromParty().getPartyId());
				msg.append(", context.FromParty.role=").append(context.getFromParty().getRole());
			}
			if (context.getToParty() != null)
			{
				msg.append(", context.ToParty.partyId=").append(context.getToParty().getPartyId());
				msg.append(", context.ToParty.role=").append(context.getToParty().getRole());
			}
			msg.append(", context.service=").append(context.getService());
			msg.append(", context.action=").append(context.getAction());
			msg.append(". context.ToParty is optional!");
			throw new ValidationException(msg.toString());
		}
	}

	private boolean equals(ActionBindingType thisPartyActionBinding, Object otherPartyActionBinding)
	{
		if (thisPartyActionBinding == otherPartyActionBinding)
			return true;
		else if (thisPartyActionBinding != null && otherPartyActionBinding != null && otherPartyActionBinding instanceof ActionBindingType)
			return thisPartyActionBinding.getId().equals(((ActionBindingType)otherPartyActionBinding).getId());
		return false;
	}

	private boolean isEmpty(Party party)
	{
		return party == null || StringUtils.isEmpty(party.getPartyId()) || StringUtils.isEmpty(party.getRole()) ;
	}

}
