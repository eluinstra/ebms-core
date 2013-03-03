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
package nl.clockwork.ebms.signing;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.ReceiverNonRepudiation;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EbMSSecSignatureGenerator implements EbMSSignatureGenerator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	//private String baseURI = org.apache.xml.security.utils.Constants.SignatureSpecNS;
	private String canonicalizationMethodAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	//private String signatureMethodAlgorithm = XMLSignature.ALGO_ID_SIGNATURE_DSA;
	private String transformAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	//private String digestAlgorithm = org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1;
	private String keyStorePath;
	private String keyStorePassword;
	private String keyAlias;
	private String keyPassword;
	private KeyStore keyStore;
	private KeyPair keyPair;

	public void init() throws GeneralSecurityException, IOException, EbMSProcessorException
	{
		keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
		keyPair = SecurityUtils.getKeyPair(keyStore,keyAlias,keyPassword);
		if (keyPair == null)
			throw new EbMSProcessorException("Cannot find key with alias: " + keyAlias);
	}

	@Override
	public void generate(CollaborationProtocolAgreement cpa, EbMSDocument document, MessageHeader messageHeader) throws EbMSProcessorException
	{
		try
		{
			//TODO: get isSigned info from CPA for all messages
			if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
			{
				PartyInfo partyInfo = CPAUtils.getPartyInfo(cpa,messageHeader.getFrom().getPartyId());
				List<DeliveryChannel> channels = CPAUtils.getSendingDeliveryChannels(partyInfo,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
				ReceiverNonRepudiation receiverNonRepudiation = CPAUtils.getReceiverNonRepudiation(channels.get(0));
				if (receiverNonRepudiation != null)
				{
					//TODO: read keyAlias from cpa (by convention (cpaId or endpoint))
					sign(keyStore,keyPair,keyAlias,document.getMessage(),document.getAttachments(),CPAUtils.getNonRepudiationProtocol(receiverNonRepudiation),CPAUtils.getSignatureAlgorithm(receiverNonRepudiation),CPAUtils.getHashFunction(receiverNonRepudiation));
				}
			}
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private void sign(KeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSAttachment> attachments, String baseURI, String signatureMethodAlgorithm, String digestAlgorithm) throws XMLSecurityException, KeyStoreException
	{
		// quickfix for error: "Cannot find ds:XPath in Transform"
		if (baseURI.equals(org.apache.xml.security.utils.Constants.SignatureSpecNS))
			baseURI = org.apache.xml.security.utils.Constants.SignatureSpecNS;
		else if (baseURI.equals(org.apache.xml.security.utils.Constants.SignatureSpec11NS))
			baseURI = org.apache.xml.security.utils.Constants.SignatureSpec11NS;

		XMLSignature signature = new XMLSignature(document,baseURI,signatureMethodAlgorithm,canonicalizationMethodAlgorithm);

		Element soapHeader = DOMUtils.getFirstChildElement(document.getDocumentElement());
		soapHeader.appendChild(signature.getElement());
		
		EbMSAttachmentResolver resolver = new EbMSAttachmentResolver(attachments);
		signature.getSignedInfo().addResourceResolver(resolver);
			
		Transforms transforms = new Transforms(document);
		transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);

		Element xpath = document.createElementNS(baseURI,org.apache.xml.security.utils.Constants._TAG_XPATH);
		xpath.setAttributeNS(org.apache.xml.security.utils.Constants.NamespaceSpecNS,"xmlns:" + Constants.NAMESPACE_PREFIX_SOAP_ENVELOPE,Constants.NAMESPACE_URI_SOAP_ENVELOPE);
		xpath.appendChild(document.createTextNode(Constants.TRANSFORM_XPATH));
		xpath.setPrefix(Constants.NAMESPACE_PREFIX_DS);

		//XPath2FilterContainer xpathC = XPath2FilterContainer.newInstanceIntersect(document,Constants.TRANSFORM_XPATH);
		//xpathC.setXPathNamespaceContext("ds",Transforms.TRANSFORM_XPATH);
		//Element xpath = xpathC.getElement();
		
		transforms.addTransform(Transforms.TRANSFORM_XPATH,xpath);
		
		transforms.addTransform(transformAlgorithm);
		
		signature.addDocument("",transforms,digestAlgorithm);
		
		for (EbMSAttachment attachment : attachments)
			signature.addDocument(Constants.CID + attachment.getContentId());
		
		signature.addKeyInfo(keyPair.getPublic());
		
		Certificate[] certificates = keyStore.getCertificateChain(alias);
	  //for (Certificate certificate : certificates)
	  //	signature.addKeyInfo((X509Certificate)certificate);
		signature.addKeyInfo((X509Certificate)certificates[0]);

		signature.sign(keyPair.getPrivate());
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
	
	public void setKeyAlias(String keyAlias)
	{
		this.keyAlias = keyAlias;
	}
	
	public void setKeyPassword(String keyPassword)
	{
		this.keyPassword = keyPassword;
	}
}
