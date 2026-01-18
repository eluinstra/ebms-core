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
package nl.clockwork.ebms.common.cpa;

import java.time.Instant;
import java.util.function.Predicate;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.validation.EbMSValidationException;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class CPAValidator
{
	@NonNull
	CPARepository cpaRepository;

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		if (!isValid(message.getMessageHeader().getCPAId(), message.getMessageHeader().getMessageData().getTimestamp()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/@cpaid", EbMSErrorCode.INCONSISTENT, "Invalid CPA."));
	}

	boolean isValid(String cpaId, Instant timestamp)
	{
		return cpaRepository.getCPA(cpaId).filter(isValidCPA(timestamp)).isPresent();
	}

	private Predicate<CollaborationProtocolAgreement> isValidCPA(Instant timestamp)
	{
		return cpa -> StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart()) >= 0
				&& timestamp.compareTo(cpa.getEnd()) <= 0;
	}

}
