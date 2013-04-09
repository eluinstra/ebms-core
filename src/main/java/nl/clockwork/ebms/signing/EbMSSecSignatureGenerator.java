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
import org.apache.xml.security.transforms.params.XPathContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class EbMSSecSignatureGenerator implements EbMSSignatureGenerator
{
	protected transient Log logger = LogFactory.getLog(getClass());
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
					sign(keyStore,keyPair,keyAlias,document.getMessage(),document.getAttachments(),CPAUtils.getSignatureAlgorithm(receiverNonRepudiation),CPAUtils.getHashFunction(receiverNonRepudiation));
				}
			}
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
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
		
		for (EbMSAttachment attachment : attachments)
			signature.addDocument(Constants.CID + attachment.getContentId());
		
		signature.addKeyInfo(keyPair.getPublic());
		
		Certificate[] certificates = keyStore.getCertificateChain(alias);
	  //for (Certificate certificate : certificates)
	  //	signature.addKeyInfo((X509Certificate)certificate);
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
