/*
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class MessagePropertiesValidator
{
	@NonNull
	CPAManager cpaManager;

	public void validate(@NonNull String cpaId, @NonNull String fromPartyId, String toPartyId) throws ValidationException
	{
		if (!cpaManager.existsCPA(cpaId))
			throw new ValidationException("No CPA found for cpaId=" + cpaId);
		if (!cpaManager.existsPartyId(cpaId, fromPartyId))
		{
			val msg = new StringBuffer();
			msg.append("No fromParty found for");
			msg.append(" cpaId=").append(cpaId);
			msg.append(", fromPartyId=").append(fromPartyId);
			throw new ValidationException(msg.toString());
		}
		if (!cpaManager.existsPartyId(cpaId, toPartyId))
		{
			val msg = new StringBuffer();
			msg.append("No toParty found for");
			msg.append(" cpaId=").append(cpaId);
			msg.append(", toPartyId=").append(toPartyId);
			throw new ValidationException(msg.toString());
		}
	}

	public void validate(MessageRequestProperties properties) throws ValidatorException
	{
		if (!cpaManager.existsCPA(properties.getCpaId()))
			throw new ValidationException("No CPA found for message.cpaId=" + properties.getCpaId());
		val fromPartyInfo = cpaManager.getFromPartyInfo(
				properties.getCpaId(),
				Party.of(properties.getFromPartyId(), properties.getFromRole()),
				properties.getService(),
				properties.getAction());
		if (!fromPartyInfo.isPresent())
			throw new ValidationException("No CanSend action found for " + properties);
		val toParty = properties.getToPartyId() != null ? Party.of(properties.getToPartyId(), properties.getToRole()) : null;
		val toPartyInfo = cpaManager.getToPartyInfo(properties.getCpaId(), toParty, properties.getService(), properties.getAction());
		if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() == null && !toPartyInfo.isPresent())
			throw new ValidationException("No CanReceive action found for " + properties);
		else if (fromPartyInfo.get().getCanSend().getOtherPartyActionBinding() != null
				&& toPartyInfo.isPresent()
				&& !equals(toPartyInfo.get().getCanReceive().getThisPartyActionBinding(), fromPartyInfo.get().getCanSend().getOtherPartyActionBinding()))
			throw new ValidationException("Action for to party does not match action for from party for " + properties);
	}

	private boolean equals(ActionBindingType thisPartyActionBinding, Object otherPartyActionBinding)
	{
		if (thisPartyActionBinding == otherPartyActionBinding)
			return true;
		else if (thisPartyActionBinding != null && otherPartyActionBinding != null && otherPartyActionBinding instanceof ActionBindingType)
			return thisPartyActionBinding.getId().equals(((ActionBindingType)otherPartyActionBinding).getId());
		return false;
	}
}
