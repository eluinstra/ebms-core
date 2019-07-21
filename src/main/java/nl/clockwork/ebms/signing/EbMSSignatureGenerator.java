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
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.XPathContainer;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.ThrowingConsumer;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

public class EbMSSignatureGenerator implements InitializingBean
{
	private CPAManager cpaManager;
	private String canonicalizationMethodAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	//private String signatureMethodAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_DSA;
	private String transformAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	//private String digestAlgorithm = org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStore keyStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		keyStore = KeyStoreManager.getKeyStore(keyStorePath,keyStorePassword);
	}
	
	public void generate(EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = message.getMessageHeader();
			CacheablePartyId fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			String service = CPAUtils.toString(messageHeader.getService());
			if (cpaManager.isNonRepudiationRequired(
					messageHeader.getCPAId(),
					fromPartyId,
					messageHeader.getFrom().getRole(),
					service,
					messageHeader.getAction()))
				sign(message);
		}
		catch (GeneralSecurityException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (XMLSecurityException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	public void generate(AckRequested ackRequested, EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			if (ackRequested != null && ackRequested.isSigned())
			{
				sign(message);
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (XMLSecurityException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private void sign(EbMSMessage message) throws EbMSProcessorException, GeneralSecurityException, XMLSecurityException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		CacheablePartyId fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
		String service = CPAUtils.toString(messageHeader.getService());
		DeliveryChannel deliveryChannel =
				cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction())
				.orElseThrow(() ->
				StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
		X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
		String alias = keyStore.getCertificateAlias(certificate);
		if (alias == null)
			throw new EbMSProcessorException(
					"No certificate found with subject \"" + certificate.getSubjectDN().getName() + "\" in keystore \"" + keyStorePath + "\"");
		KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,alias,keyStorePassword);
		String signatureAlgorithm = CPAUtils.getSignatureAlgorithm(deliveryChannel);
		String hashFunction = CPAUtils.getHashFunction(deliveryChannel);
		sign(keyStore,keyPair,alias,message.getMessage(),message.getAttachments(),signatureAlgorithm,hashFunction);
	}
	
	private void sign(KeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSAttachment> attachments, String signatureMethodAlgorithm, String digestAlgorithm) throws XMLSecurityException, KeyStoreException
	{
		XMLSignature signature = new XMLSignature(document,null,signatureMethodAlgorithm,canonicalizationMethodAlgorithm);

		Element soapHeader = DOMUtils.getFirstChildElement(document.getDocumentElement());
		soapHeader.appendChild(signature.getElement());
		
		EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
		signature.getSignedInfo().addResourceResolver(resolver);
			
		Transforms transforms = new Transforms(document);
		transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
		transforms.addTransform(Transforms.TRANSFORM_XPATH,getXPathTransform(document));
		transforms.addTransform(transformAlgorithm);
		
		signature.addDocument("",transforms,digestAlgorithm);
		
		attachments.stream()
		.forEach(ThrowingConsumer.throwingConsumerWrapper(a -> signature.addDocument(Constants.CID + a.getContentId(),null,digestAlgorithm)));
		
		signature.addKeyInfo(keyPair.getPublic());
		
		Certificate[] certificates = keyStore.getCertificateChain(alias);
		//Stream.of(certificates).forEach(ThrowingConsumer.throwingConsumerWrapper(c -> signature.addKeyInfo((X509Certificate)c)));
		signature.addKeyInfo((X509Certificate)certificates[0]);

		signature.sign(keyPair.getPrivate());
	}
	
	private NodeList getXPathTransform(Document document) throws XMLSecurityException
	{
		String prefix = document.lookupPrefix(Constants.NSURI_SOAP_ENVELOPE);
		prefix = prefix == null ? "" : prefix + ":";
		XPathContainer container = new XPathContainer(document);
		//container.setXPathNamespaceContext(prefix,Constants.NSURI_SOAP_ENVELOPE);
		container.setXPath("not(ancestor-or-self::node()[@" + prefix + "actor=\"urn:oasis:names:tc:ebxml-msg:actor:nextMSH\"]|ancestor-or-self::node()[@" + prefix + "actor=\"" + Constants.NSURI_SOAP_NEXT_ACTOR + "\"])");
		return container.getElementPlusReturns();
	}
	
	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setCanonicalizationMethodAlgorithm(String canonicalizationMethodAlgorithm)
	{
		this.canonicalizationMethodAlgorithm = canonicalizationMethodAlgorithm;
	}
	
	public void setTransformAlgorithm(String transformAlgorithm)
	{
		this.transformAlgorithm = transformAlgorithm;
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
