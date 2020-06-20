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
package nl.clockwork.ebms.signing;

import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.utils.Constants;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.ReferenceType;
import org.w3c.dom.Element;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.util.SecurityUtils;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSSignatureValidator
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSTrustStore trustStore;

	public void validate(EbMSDocument document, EbMSMessage message) throws ValidatorException, ValidationException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			if (cpaManager.isNonRepudiationRequired(
					messageHeader.getCPAId(),
					messageHeader.getFrom().getPartyId(),
					messageHeader.getFrom().getRole(),
					CPAUtils.toString(messageHeader.getService()),
					messageHeader.getAction()))
			{
				val signatureNodeList = document.getMessage().getElementsByTagNameNS(Constants.SignatureSpecNS,Constants._TAG_SIGNATURE);
				if (signatureNodeList.getLength() > 0)
				{
					val certificate = getCertificate(messageHeader);
					if (certificate != null)
					{
						val timestamp = messageHeader.getMessageData().getTimestamp() == null ? Instant.now() : messageHeader.getMessageData().getTimestamp();
						SecurityUtils.validateCertificate(trustStore,certificate,timestamp);
						if (!verify(certificate,(Element)signatureNodeList.item(0),message.getAttachments()))
							throw new ValidationException("Invalid Signature!");
					}
					else
						throw new ValidationException("Certificate not found!");
				}
				else
					throw new ValidationException("Signature not found!");
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
		catch (XMLSecurityException e)
		{
			throw new ValidationException(e);
		}
	}

	public void validate(EbMSDocument responseDocument, EbMSMessage requestMessage, EbMSAcknowledgment responseMessage) throws ValidatorException, ValidationException
	{
		try
		{
			if (requestMessage.getAckRequested().isSigned())
			{
				val signatureNodeList = responseDocument.getMessage().getElementsByTagNameNS(Constants.SignatureSpecNS,Constants._TAG_SIGNATURE);
				if (signatureNodeList.getLength() > 0)
				{
					val certificate = getCertificate(responseMessage.getMessageHeader());
					if (certificate != null)
					{
						val date = responseMessage.getMessageHeader().getMessageData().getTimestamp() == null ? Instant.now() : responseMessage.getMessageHeader().getMessageData().getTimestamp();
						SecurityUtils.validateCertificate(trustStore,certificate,date);
						if (!verify(certificate,(Element)signatureNodeList.item(0),Collections.emptyList()))
							throw new ValidationException("Invalid Signature!");
						validateSignatureReferences(requestMessage,responseMessage);
					}
					else
						throw new ValidationException("Certificate not found!");
				}
				else
					throw new ValidationException("Signature not found!");
			}
		}
		catch (KeyStoreException e)
		{
			throw new ValidatorException(e);
		}
		catch (XMLSecurityException e)
		{
			throw new ValidationException(e);
		}
	}

	private boolean verify(X509Certificate certificate, Element signatureElement, List<EbMSAttachment> attachments) throws XMLSignatureException, XMLSecurityException
	{
		val signature = new XMLSignature(signatureElement,org.apache.xml.security.utils.Constants.SignatureSpecNS);
		val resolver = new EbMSAttachmentResolver(attachments);
		signature.addResourceResolver(resolver);
		return signature.checkSignatureValue(certificate);
	}

	private X509Certificate getCertificate(MessageHeader messageHeader)
	{
		try
		{
			val service = CPAUtils.toString(messageHeader.getService());
			val deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			if (deliveryChannel != null)
				return CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
			return null;
		}
		catch (CertificateException e)
		{
			log.warn("",e);
			return null;
		}
	}

	private void validateSignatureReferences(EbMSMessage requestMessage, EbMSAcknowledgment responseMessage) throws ValidationException
	{
		if (requestMessage.getSignature().getSignedInfo().getReference() == null || requestMessage.getSignature().getSignedInfo().getReference().size() == 0)
			throw new ValidationException("No signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		if (responseMessage.getAcknowledgment().getReference() == null || responseMessage.getAcknowledgment().getReference().size() == 0)
			throw new ValidationException("No signature references found in response message " + responseMessage.getMessageHeader().getMessageData().getMessageId());
		if (requestMessage.getSignature().getSignedInfo().getReference().size() != responseMessage.getAcknowledgment().getReference().size())
			throw new ValidationException("Nr of signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " and response message " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " do not match");
//		if (responseMessage.getAcknowledgment().getReference().stream()
//				.distinct()
//				.filter(r -> requestMessage.getSignature().getSignedInfo().getReference().contains(r))
//				.collect(Collectors.toSet()).size() > 0)
//			throw new ValidationException("Signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " and response message " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " do not match");
		if (requestMessage.getSignature().getSignedInfo().getReference().stream()
				.filter(r -> !contains(responseMessage.getAcknowledgment().getReference(),r))
				.collect(Collectors.toSet()).size() > 0)
			throw new ValidationException("Signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " and response message " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " do not match");
	}

	private boolean contains(List<ReferenceType> requestReferences, ReferenceType responseReference)
	{
		return requestReferences.stream().anyMatch(r -> responseReference.getURI().equals(r.getURI())
				&& Arrays.equals(r.getDigestValue(),responseReference.getDigestValue()));
	}
}
