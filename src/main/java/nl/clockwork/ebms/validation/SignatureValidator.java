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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class SignatureValidator
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSSignatureValidator ebMSSignatureValidator;

	public void validate(EbMSMessage message) throws ValidatorException
	{
		val messageHeader = message.getMessageHeader();
		val signature = message.getSignature();
		
		val service = CPAUtils.toString(messageHeader.getService());
		val deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
		if (cpaManager.isNonRepudiationRequired(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()))
		{
			if (signature == null)
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/Signature",EbMSErrorCode.SECURITY_FAILURE,"Signature not found."));
			val reference = signature.getSignedInfo().getReference().stream()
					.filter(r -> !CPAUtils.getHashFunction(deliveryChannel).equals(r.getDigestMethod().getAlgorithm())).findFirst();
			if (reference.isPresent())
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/Signature/SignedInfo/Reference[@URI='" + reference.get().getURI() + "']/DigestMethod/@Algorithm",EbMSErrorCode.SECURITY_FAILURE,"Invalid DigestMethod."));
			if (!CPAUtils.getSignatureAlgorithm(deliveryChannel).equals(signature.getSignedInfo().getSignatureMethod().getAlgorithm()))
				throw new EbMSValidationException(
						EbMSMessageUtils.createError("//Header/Signature/SignedInfo/SignatureMethod/@Algorithm",EbMSErrorCode.SECURITY_FAILURE,"Invalid SignatureMethod."));
		}
	}

	public void validateSignature(EbMSDocument document, EbMSMessage message) throws ValidatorException
	{
		try
		{
			ebMSSignatureValidator.validate(document,message);
		}
		catch (ValidationException e)
		{
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/Signature",EbMSErrorCode.SECURITY_FAILURE,e.getMessage()));
		}
	}

	public void validate(EbMSDocument responseDocument, EbMSMessage requestMessage, EbMSAcknowledgment responseMessage) throws ValidationException, ValidatorException
	{
		ebMSSignatureValidator.validate(responseDocument,requestMessage,responseMessage);
	}
}
