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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;
import org.w3._2000._09.xmldsig.ReferenceType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EbMSSignatureValidator implements InitializingBean
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private CPAManager cpaManager;
	private String trustStorePath;
	private String trustStorePassword;
	private KeyStore trustStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		trustStore = KeyStoreManager.getKeyStore(trustStorePath,trustStorePassword);
	}

	public void validate(EbMSMessage message) throws ValidatorException, ValidationException
	{
		try
		{
			if (cpaManager.isNonRepudiationRequired(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction()))
			{
				NodeList signatureNodeList = message.getMessage().getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
				if (signatureNodeList.getLength() > 0)
				{
					X509Certificate certificate = getCertificate(message.getMessageHeader());
					if (certificate != null)
					{
						SecurityUtils.validateCertificate(trustStore,certificate,message.getMessageHeader().getMessageData().getTimestamp() == null ? new Date() : message.getMessageHeader().getMessageData().getTimestamp());
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

	public void validate(EbMSMessage requestMessage, EbMSMessage responseMessage) throws ValidatorException, ValidationException
	{
		try
		{
			if (requestMessage.getAckRequested().isSigned())
			{
				NodeList signatureNodeList = responseMessage.getMessage().getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
				if (signatureNodeList.getLength() > 0)
				{
					X509Certificate certificate = getCertificate(responseMessage.getMessageHeader());
					if (certificate != null)
					{
						SecurityUtils.validateCertificate(trustStore,certificate,responseMessage.getMessageHeader().getMessageData().getTimestamp() == null ? new Date() : responseMessage.getMessageHeader().getMessageData().getTimestamp());
						if (!verify(certificate,(Element)signatureNodeList.item(0),new ArrayList<EbMSAttachment>()))
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
		XMLSignature signature = new XMLSignature(signatureElement,org.apache.xml.security.utils.Constants.SignatureSpecNS);
		EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
		signature.addResourceResolver(resolver);
		return signature.checkSignatureValue(certificate);
	}

	private X509Certificate getCertificate(MessageHeader messageHeader)
	{
		try
		{
			DeliveryChannel deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getFrom().getPartyId()),messageHeader.getFrom().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
			if (deliveryChannel != null)
				return CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
			return null;
		}
		catch (CertificateException e)
		{
			logger.warn("",e);
			return null;
		}
	}

	private void validateSignatureReferences(EbMSMessage requestMessage, EbMSMessage responseMessage) throws ValidationException
	{
		if (requestMessage.getSignature().getSignedInfo().getReference() == null || requestMessage.getSignature().getSignedInfo().getReference().size() == 0)
			throw new ValidationException("No signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId());
		if (responseMessage.getAcknowledgment().getReference() == null || responseMessage.getAcknowledgment().getReference().size() == 0)
			throw new ValidationException("No signature references found in response message " + responseMessage.getMessageHeader().getMessageData().getMessageId());
		if (requestMessage.getSignature().getSignedInfo().getReference().size() != responseMessage.getAcknowledgment().getReference().size())
			throw new ValidationException("Nr of signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " and response message " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " do not match");
		for (ReferenceType requestReference : requestMessage.getSignature().getSignedInfo().getReference())
		{
			boolean found = false;
			for (ReferenceType responseReference : responseMessage.getAcknowledgment().getReference())
			{
				found = equals(requestReference,responseReference);
				if (found) break;
			}
			if (!found)
				throw new ValidationException("Signature references found in request message " + requestMessage.getMessageHeader().getMessageData().getMessageId() + " and response message " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " do not match");
		}
				
	}

	private boolean equals(ReferenceType requestReference, ReferenceType responseReference)
	{
		return requestReference.getURI().equals(responseReference.getURI())
				&& Arrays.equals(requestReference.getDigestValue(),responseReference.getDigestValue());
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setTrustStorePath(String trustStorePath)
	{
		this.trustStorePath = trustStorePath;
	}
	
	public void setTrustStorePassword(String trustStorePassword)
	{
		this.trustStorePassword = trustStorePassword;
	}

}
