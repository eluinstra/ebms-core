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
package nl.clockwork.mule.ebms.cxf;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathConstants;

import nl.clockwork.ebms.SignatureManager;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.common.util.XMLUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.Signature;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.xml.EbXMLNamespaceContext;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EbMSSecSignatureInInterceptor extends AbstractSoapInterceptor
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private String keyStorePath;
	private String keyStorePassword;

	public EbMSSecSignatureInInterceptor()
	{
		super(Phase.POST_STREAM);
		addAfter(StaxInInterceptor.class.getName());
		//FIXME move to mule init bean??? 
		org.apache.xml.security.Init.init();
	}

	private boolean verify(X509Certificate certificate, NodeList signatureNodeList, List<EbMSAttachment> attachments) throws XMLSignatureException, XMLSecurityException, CertificateExpiredException, CertificateNotYetValidException, KeyStoreException
	{
		XMLSignature signature = new XMLSignature((Element)signatureNodeList.item(0),org.apache.xml.security.utils.Constants.SignatureSpecNS);
		EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
		signature.addResourceResolver(resolver);
		return signature.checkSignatureValue(certificate);
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
		try
		{
			KeyStore keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);

			XMLStreamReader reader = message.getContent(XMLStreamReader.class);
			Document document = StaxUtils.read(reader);
			XMLStreamReader copy = StaxUtils.createXMLStreamReader(document);
			message.setContent(XMLStreamReader.class,copy);

			List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
			if (message.getAttachments() != null)
				for (Attachment attachment : message.getAttachments())
					attachments.add(new EbMSAttachment(attachment.getDataHandler().getDataSource(),attachment.getId(),attachment.getDataHandler().getName()));

			NodeList signatureNodeList = document.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
			if (signatureNodeList.getLength() > 0)
			{
				boolean isValid = false;
				X509Certificate certificate = getCertificate(document);
				if (certificate != null)
				{
					isValid = validateCertificate(keyStore,certificate,new Date()/*TODO get date from message???*/);
					if (!isValid)
						logger.info("Certificate invalid.");
					else
					{
						isValid = verify(certificate,signatureNodeList,attachments);
						logger.info("Signature" + (isValid ? " " : " in") + "valid.");
					}
				}
				SignatureManager.set(new Signature(certificate,XMLMessageBuilder.getInstance(SignatureType.class).handle(signatureNodeList.item(0)),isValid));
			}
			else
				SignatureManager.set(null);
		}
		catch (Exception e)
		{
			throw new Fault(e);
		}
	}

	private X509Certificate getCertificate(Document document)
	{
		try
		{
			Node n = (Node)XMLUtils.executeXPathQuery(new EbXMLNamespaceContext(),document,"/soap:Envelope/soap:Header/ebxml:MessageHeader",XPathConstants.NODE);
			MessageHeader messageHeader = XMLMessageBuilder.getInstance(MessageHeader.class).handle(n);
			CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
			if (cpa != null)
			{
				PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
				if (partyInfo != null)
				{
					List<DeliveryChannel> channels = CPAUtils.getDeliveryChannels(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
					if (channels.size() == 1)
					{
						nl.clockwork.ebms.model.cpp.cpa.Certificate c = CPAUtils.getCertificate(channels.get(0));
						if (c == null)
							return null;
						else
							return CPAUtils.getX509Certificate(c);
					}
				}
			}
			return null;
		}
		catch (Exception e)
		{
			logger.warn("",e);
			return null;
		}
	}
/*
	private X509Certificate getCertificate(Document document)
	{
		try
		{
			NodeList signatureNodeList = document.getElementsByTagNameNS(org.apache.xml.security.utils.Constants.SignatureSpecNS,org.apache.xml.security.utils.Constants._TAG_SIGNATURE);
			XMLSignature signature = new XMLSignature((Element)signatureNodeList.item(0),org.apache.xml.security.utils.Constants.SignatureSpecNS);
			return signature.getKeyInfo().getX509Certificate();
		}
		catch (Exception e)
		{
			logger.warn("",e);
			return null;
		}
	}
*/
	private boolean validateCertificate(KeyStore keyStore, X509Certificate certificate, Date date) throws KeyStoreException
	{
		try
		{
			certificate.checkValidity(date);
		}
		catch (Exception e)
		{
			return false;
		}
		Enumeration<String> aliases = keyStore.aliases();
		while (aliases.hasMoreElements())
		{
			try
			{
				Certificate c = keyStore.getCertificate(aliases.nextElement());
				certificate.verify(c.getPublicKey());
				return true;
			}
			catch (KeyStoreException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				logger.debug("",e);
			}
		}
		return false;
	}
/*
	private boolean verifyCertificate(XMLSignature signature, X509Certificate certificate)
	{
		boolean result = false;
		try
		{
			X509Certificate c = signature.getKeyInfo().getX509Certificate();
			result |= certificate.equals(c);
		}
		catch (KeyResolverException e)
		{
			logger.info("",e);
		}
		try
		{
			PublicKey publicKey = signature.getKeyInfo().getPublicKey();
			result |= certificate.getPublicKey().getAlgorithm().equals(publicKey.getAlgorithm())
				&& certificate.getPublicKey().getFormat().equals(publicKey.getFormat())
				&& certificate.getPublicKey().getEncoded().equals(publicKey.getEncoded());
		}
		catch (KeyResolverException e)
		{
			logger.info("",e);
		}
		return result;
	}
*/
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
	
	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}
	
	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}
}
