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

import java.util.Calendar;
import java.util.GregorianCalendar;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class CPAValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public void validate(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, GregorianCalendar timestamp) throws EbMSValidationException
	{
		if (!cpaExists(cpa,messageHeader))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"CPA not found."));
		if (!CPAUtils.isValid(cpa,timestamp))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Invalid CPA."));
	}

	public void validate(CollaborationProtocolAgreement cpa) throws ValidatorException
	{
		if (!"2_0b".equals(cpa.getVersion()))
			//throw new ValidationException("CPA version " + cpa.getVersion() + " detected! CPA version 2_0b expected.");
			logger.warn("CPA version " + cpa.getVersion() + " detected! CPA version 2_0b expected.");
		if ("proposed".equals(cpa.getStatus()))
			throw new ValidationException("CPA Status is proposed!");
		if (cpa.getStart().compare(cpa.getEnd()) > 0)
			throw new ValidationException("CPA End date before Start date!");
		if (Calendar.getInstance().compareTo(cpa.getEnd().toGregorianCalendar()) > 0)
			throw new ValidationException("CPA expired on " + cpa.getEnd());
		if (cpa.getConversationConstraints() != null)
			//throw new ValidationException("CPA Conversation Constraints not supported!");
			logger.warn("CPA Conversation Constraints not supported!");
		if (cpa.getSignature() != null)
			//throw new ValidationException("CPA Signature not supported!");
			logger.warn("CPA Signature not supported!");
		if (cpa.getPartyInfo().size() != 2)
			throw new ValidationException(cpa.getPartyInfo().size() + " part" + (cpa.getPartyInfo().size() == 1 ? "y" : "ies") + " defined!");
		if (cpa.getPartyInfo().get(0).getPartyName().equals(cpa.getPartyInfo().get(1).getPartyName()))
			throw new ValidationException("PartyName " + cpa.getPartyInfo().get(0).getPartyName() + " not unique!");
		
		//CanSend/Receive
		//TimeToLive == ((Retries + 1) * RetryInterval); PersistDuration >= TimeToLive
		//1 channel per action
		//signatures
		//encryption
		//MessageOrder
		//Packaging.ComponentList.Encapsulation
	}
	
	private boolean cpaExists(CollaborationProtocolAgreement cpa, MessageHeader messageHeader)
	{
		return cpa != null && cpa.getCpaid().equals(messageHeader.getCPAId());
	}
	
}
