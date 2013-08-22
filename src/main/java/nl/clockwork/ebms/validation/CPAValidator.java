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
package nl.clockwork.ebms.validation;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;

public class CPAValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public boolean isValid(ErrorList errorList, CollaborationProtocolAgreement cpa, MessageHeader messageHeader, GregorianCalendar timestamp)
	{
		if (!cpaExists(cpa,messageHeader))
		{
			errorList.getError().add(EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"CPA not found."));
			errorList.setHighestSeverity(SeverityType.ERROR);
			return false;
		}
		if (!CPAUtils.isValid(cpa,timestamp))
		{
			errorList.getError().add(EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Invalid CPA."));
			errorList.setHighestSeverity(SeverityType.ERROR);
			return false;
		}
		return true;
	}

	public void validate(CollaborationProtocolAgreement cpa) throws ValidatorException
	{
		//if (!"2.0a".equals(cpa.getVersion()))
		//	throw new ValidationException("CPA version " + cpa.getVersion() + " detected. CPA version 2.0a expected.");
		if ("proposed".equals(cpa.getStatus()))
			throw new ValidationException("CPA not agreed to by both Parties.");
		if (cpa.getStart().compare(cpa.getEnd()) > 0)
			throw new ValidationException("CPA End Date bofre CPA Start Date!");
		if (Calendar.getInstance().compareTo(cpa.getEnd().toGregorianCalendar()) > 0)
			throw new ValidationException("CPA already expired!");
		if (cpa.getConversationConstraints() != null)
			throw new ValidationException("CPA Conversation Constraints not supported!");
		//CanSend/Receive
		//nr of channels
		//signatures
		//encryption
		//MessageOrder
		//Packaging.ComponentList.Encapsulation
		if (cpa.getSignature() != null)
			throw new ValidationException("CPA Signature not supported!");
	}
	
	private boolean cpaExists(CollaborationProtocolAgreement cpa, MessageHeader messageHeader)
	{
		return cpa != null && cpa.getCpaid().equals(messageHeader.getCPAId());
	}
	
}
