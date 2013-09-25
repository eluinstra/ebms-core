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
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.ReferenceType;
import org.w3._2000._09.xmldsig.SignatureType;
import org.w3c.dom.Document;

public class SignatureTypeValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSSignatureValidator ebMSSignatureValidator;

	public SignatureTypeValidator(EbMSSignatureValidator ebMSSignatureValidator)
	{
		this.ebMSSignatureValidator = ebMSSignatureValidator;
	}

	public void validate(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, Document document, List<EbMSAttachment> attachments) throws ValidatorException
	{
		try
		{
			ebMSSignatureValidator.validate(cpa,messageHeader,document,attachments);
		}
		catch (ValidationException e)
		{
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),e.getMessage()));
		}
	}
	
	public void validate(CollaborationProtocolAgreement cpa, MessageHeader messageHeader, SignatureType signature) throws ValidatorException
	{
		PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
		DeliveryChannel deliveryChannel = CPAUtils.getSendingDeliveryChannel(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
		if (CPAUtils.isSigned(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
		{
			if (signature == null)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Signature",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Signature not found."));
			List<ReferenceType> references = signature.getSignedInfo().getReference();
			for (ReferenceType reference : references)
				if (!CPAUtils.getHashFunction(deliveryChannel).equals(reference.getDigestMethod().getAlgorithm()))
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Signature/SignedInfo/Reference[@URI='" + reference.getURI() + "']/DigestMethod[@Algorithm]",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Invalid DigestMethod."));
			if (!CPAUtils.getSignatureAlgorithm(deliveryChannel).equals(signature.getSignedInfo().getSignatureMethod().getAlgorithm()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Signature/SignedInfo/SignatureMethod[@Algorithm]",Constants.EbMSErrorCode.SECURITY_FAILURE.errorCode(),"Invalid SignatureMethod."));
		}
	}
	
	public void setEbMSSignatureValidator(EbMSSignatureValidator ebMSSignatureValidator)
	{
		this.ebMSSignatureValidator = ebMSSignatureValidator;
	}

}
