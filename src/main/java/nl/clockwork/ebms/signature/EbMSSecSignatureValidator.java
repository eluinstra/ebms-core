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
package nl.clockwork.ebms.signature;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EbMSSecSignatureValidator implements EbMSSignatureValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private String trustStorePath;
	private String trustStorePassword;

	public EbMSSecSignatureValidator()
	{
		org.apache.xml.security.Init.init();
	}
	
	@Override
	public void validate(CollaborationProtocolAgreement cpa, EbMSDocument document, MessageHeader messageHeader) throws ValidatorException, ValidationException
	{
		try
		{
			PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
			if (CPAUtils.isSigned(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
			{
				KeyStore trustStore = SecurityUtils.loadKeyStore(trustStorePath,trustStorePassword);
				NodeList signatureNodeList = document.getMessage().getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
				if (signatureNodeList.getLength() > 0)
				{
					X509Certificate certificate = getCertificate(cpa,document.getMessage(),messageHeader);
					if (certificate != null)
					{
						validateCertificate(trustStore,certificate,messageHeader.getMessageData().getTimestamp() == null ? new Date() : messageHeader.getMessageData().getTimestamp().toGregorianCalendar().getTime());
						if (!verify(certificate,(Element)signatureNodeList.item(0),document.getAttachments()))
							throw new ValidationException("Invalid Signature!");
					}
					else
						throw new ValidationException("Certificate not found!");
				}
				else
					throw new ValidationException("Signature not found!");
			}
		}
		catch (GeneralSecurityException | IOException e)
		{
			throw new ValidatorException(e);
		}
		catch (XMLSecurityException e)
		{
			throw new ValidationException(e);
		}
	}

	private boolean verify(X509Certificate certificate, Element signatureElement, List<EbMSAttachment> attachments) throws XMLSignatureException, XMLSecurityException, CertificateExpiredException, CertificateNotYetValidException, KeyStoreException
	{
		XMLSignature signature = new XMLSignature(signatureElement,org.apache.xml.security.utils.Constants.SignatureSpecNS);
		EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
		signature.addResourceResolver(resolver);
		return signature.checkSignatureValue(certificate);
	}

	private X509Certificate getCertificate(CollaborationProtocolAgreement cpa, Document document, MessageHeader messageHeader)
	{
		try
		{
			if (cpa != null)
			{
				PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
				if (partyInfo != null)
				{
					DeliveryChannel deliveryChannel = CPAUtils.getSendingDeliveryChannel(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
					if (deliveryChannel != null)
						return CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
				}
			}
			return null;
		}
		catch (CertificateException e)
		{
			logger.warn("",e);
			return null;
		}
	}

//	private X509Certificate getCertificate(Document document)
//	{
//		try
//		{
//			NodeList signatureNodeList = document.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
//			XMLSignature signature = new XMLSignature((Element)signatureNodeList.item(0),org.apache.xml.security.utils.Constants.SignatureSpecNS);
//			return signature.getKeyInfo().getX509Certificate();
//		}
//		catch (XMLSecurityException e)
//		{
//			logger.warn("",e);
//			return null;
//		}
//	}

	private void validateCertificate(KeyStore trustStore, X509Certificate certificate, Date date) throws KeyStoreException, ValidationException
	{
		try
		{
			certificate.checkValidity(date);
		}
		catch (CertificateExpiredException | CertificateNotYetValidException e)
		{
			throw new ValidationException(e);
		}
		Enumeration<String> aliases = trustStore.aliases();
		while (aliases.hasMoreElements())
		{
			try
			{
				Certificate c = trustStore.getCertificate(aliases.nextElement());
				if (c instanceof X509Certificate)
					if (certificate.getIssuerDN().getName().equals(((X509Certificate)c).getSubjectDN().getName()))
					{
						certificate.verify(c.getPublicKey());
						return;
					}
			}
			catch (GeneralSecurityException e)
			{
				logger.trace("",e);
			}
		}
		throw new ValidationException("Certificate " + certificate.getIssuerDN() + " not found!");
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
