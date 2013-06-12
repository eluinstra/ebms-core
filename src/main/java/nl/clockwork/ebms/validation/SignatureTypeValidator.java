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

import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.SeverityType;
import nl.clockwork.ebms.model.xml.dsig.ReferenceType;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SignatureTypeValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSSignatureValidator ebMSSignatureValidator;

	public SignatureTypeValidator(EbMSSignatureValidator ebMSSignatureValidator)
	{
		this.ebMSSignatureValidator = ebMSSignatureValidator;
	}

	public boolean isValid(ErrorList errorList, CollaborationProtocolAgreement cpa, EbMSDocument document, MessageHeader messageHeader) throws ValidatorException
	{
		try
		{
			ebMSSignatureValidator.validate(cpa,document,messageHeader);
		}
		catch (ValidationException e)
		{
			errorList.getError().add(EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),e.getMessage()));
			errorList.setHighestSeverity(SeverityType.ERROR);
			return false;
		}
		return true;
	}
	
	public boolean isValid(ErrorList errorList, CollaborationProtocolAgreement cpa, MessageHeader messageHeader, SignatureType signature) throws ValidatorException
	{
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getSendingDeliveryChannel(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
		if (CPAUtils.isSigned(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
		{
			if (signature == null)
			{
				errorList.getError().add(EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Signature not found."));
				errorList.setHighestSeverity(SeverityType.ERROR);
				return false; 
			}
			List<ReferenceType> references = signature.getSignedInfo().getReference();
			for (ReferenceType reference : references)
				if (!CPAUtils.getHashFunction(deliveryChannel).equals(reference.getDigestMethod().getAlgorithm()))
				{
					errorList.getError().add(EbMSMessageUtils.createError("//Header/Signature/SignedInfo/Reference[@URI='" + reference.getURI() + "']/DigestMethod[@Algorithm]",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Invalid DigestMethod."));
					errorList.setHighestSeverity(SeverityType.ERROR);
					return false; 
				}
			if (!CPAUtils.getSignatureAlgorithm(deliveryChannel).equals(signature.getSignedInfo().getSignatureMethod().getAlgorithm()))
			{
				errorList.getError().add(EbMSMessageUtils.createError("//Header/Signature/SignedInfo/SignatureMethod[@Algorithm]",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Invalid SignatureMethod."));
				errorList.setHighestSeverity(SeverityType.ERROR);
				return false; 
			}
		}
		return true;
	}
	
	public void setEbMSSignatureValidator(EbMSSignatureValidator ebMSSignatureValidator)
	{
		this.ebMSSignatureValidator = ebMSSignatureValidator;
	}

}
