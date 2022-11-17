/*
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
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.XPathContainer;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSResponseMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.util.SecurityUtils;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.xml.dsig.EbMSAttachmentResolver;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSSignatureGenerator
{
	String canonicalizationMethodAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	String transformAlgorithm = Transforms.TRANSFORM_C14N_OMIT_COMMENTS;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSKeyStore keyStore;

	public void generate(EbMSDocument document, EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			if (cpaManager.isSendingNonRepudiationRequired(
					messageHeader.getCPAId(),
					messageHeader.getFrom().getPartyId(),
					messageHeader.getFrom().getRole(),
					CPAUtils.toString(messageHeader.getService()),
					messageHeader.getAction()))
				sign(document,message,message.getAttachments());
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

	public void generate(AckRequested ackRequested, EbMSDocument document, EbMSResponseMessage message) throws EbMSProcessorException
	{
		try
		{
			if (ackRequested != null && ackRequested.isSigned())
			{
				sign(document,message,Collections.emptyList());
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

	private void sign(EbMSDocument document, EbMSBaseMessage message, List<EbMSAttachment> attachments) throws EbMSProcessorException, GeneralSecurityException, XMLSecurityException
	{
		val messageHeader = message.getMessageHeader();
		val service = CPAUtils.toString(messageHeader.getService());
		val deliveryChannel = cpaManager.getSendDeliveryChannel(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
				.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
		val certificate = CPAUtils.getX509Certificate(CPAUtils.getSigningCertificate(deliveryChannel));
		if (certificate == null)
			throw new EbMSProcessingException(
					"No signing certificate found for deliveryChannel \"" + deliveryChannel.getChannelId() + "\" in CPA \"" + messageHeader.getCPAId() + "\"");
		val alias = keyStore.getCertificateAlias(certificate);
		if (alias == null)
			throw new EbMSProcessorException(
					"No certificate found with subject \"" + certificate.getSubjectDN().getName() + "\" in keystore \"" + keyStore.getPath() + "\"");
		val keyPair = SecurityUtils.getKeyPair(keyStore,alias,keyStore.getKeyPassword());
		val signatureAlgorithm = CPAUtils.getSignatureAlgorithm(deliveryChannel);
		val hashFunction = CPAUtils.getHashFunction(deliveryChannel);
		sign(keyStore,keyPair,alias,document.getMessage(),attachments,signatureAlgorithm,hashFunction);
	}
	
	private void sign(EbMSKeyStore keyStore, KeyPair keyPair, String alias, Document document, List<EbMSAttachment> attachments, String signatureMethodAlgorithm, String digestAlgorithm) throws XMLSecurityException, KeyStoreException
	{
		val signature = createSignature(document,signatureMethodAlgorithm);
		appendSignature(document,signature);
		addAttachmentResolver(signature,attachments);
		signature.addDocument("",createTransforms(document),digestAlgorithm);
		for (val attachment: attachments)
			signature.addDocument(Constants.CID + attachment.getContentId(),null,digestAlgorithm);
		signature.addKeyInfo(keyPair.getPublic());
		addCertificate(signature,keyStore,alias);
		signature.sign(keyPair.getPrivate());
	}

	private XMLSignature createSignature(Document document, String signatureMethodAlgorithm) throws XMLSecurityException
	{
		return new XMLSignature(document,null,signatureMethodAlgorithm,canonicalizationMethodAlgorithm);
	}

	private void appendSignature(Document document, final XMLSignature signature)
	{
		val soapHeader = DOMUtils.getFirstChildElement(document.getDocumentElement());
		soapHeader.appendChild(signature.getElement());
	}

	private void addAttachmentResolver(final XMLSignature signature, List<EbMSAttachment> attachments)
	{
		val resolver = new EbMSAttachmentResolver(attachments);
		signature.getSignedInfo().addResourceResolver(resolver);
	}

	private Transforms createTransforms(Document document) throws XMLSecurityException
	{
		val result = new Transforms(document);
		result.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
		result.addTransform(Transforms.TRANSFORM_XPATH,getXPathTransform(document));
		result.addTransform(transformAlgorithm);
		return result;
	}
	
	private void addCertificate(final XMLSignature signature, EbMSKeyStore keyStore, String alias) throws KeyStoreException, XMLSecurityException
	{
		val certificates = keyStore.getCertificateChain(alias);
		//Stream.of(certificates).forEach(ThrowingConsumer.throwingConsumerWrapper(c -> signature.addKeyInfo((X509Certificate)c)));
		signature.addKeyInfo((X509Certificate)certificates[0]);
	}

	private NodeList getXPathTransform(Document document) throws XMLSecurityException
	{
		val rawPrefix = document.lookupPrefix(Constants.NSURI_SOAP_ENVELOPE);
		val prefix = rawPrefix == null ? "" : rawPrefix + ":";
		val container = new XPathContainer(document);
		//container.setXPathNamespaceContext(prefix,Constants.NSURI_SOAP_ENVELOPE);
		container.setXPath("not(ancestor-or-self::node()[@" + prefix + "actor=\"urn:oasis:names:tc:ebxml-msg:actor:nextMSH\"]|ancestor-or-self::node()[@" + prefix + "actor=\"" + Constants.NSURI_SOAP_NEXT_ACTOR + "\"])");
		return container.getElementPlusReturns();
	}
}
