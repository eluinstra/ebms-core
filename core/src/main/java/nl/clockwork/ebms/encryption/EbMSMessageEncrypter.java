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
package nl.clockwork.ebms.encryption;

import java.io.IOException;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import javax.crypto.SecretKey;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.security.EbMSTrustStore;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.util.SecurityUtils;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.KeyName;
import org.apache.xml.security.utils.EncryptionConstants;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EbMSMessageEncrypter
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSTrustStore trustStore;

	public void encrypt(EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			if (cpaManager.isSendingConfidential(
					messageHeader.getCPAId(),
					messageHeader.getFrom().getPartyId(),
					messageHeader.getFrom().getRole(),
					service,
					messageHeader.getAction()))
			{
				val toPartyId = messageHeader.getTo().getPartyId();
				val deliveryChannel =
						cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(), toPartyId, messageHeader.getTo().getRole(), service, messageHeader.getAction())
								.orElseThrow(
										() -> StreamUtils.illegalStateException(
												"ReceiveDeliveryChannel",
												messageHeader.getCPAId(),
												toPartyId,
												messageHeader.getTo().getRole(),
												service,
												messageHeader.getAction()));
				val certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				SecurityUtils.validateCertificate(trustStore, certificate, Instant.now());
				val encryptionAlgorithm = CPAUtils.getEncryptionAlgorithm(deliveryChannel);
				message.getAttachments().replaceAll(a -> encrypt(createDocument(), certificate, encryptionAlgorithm, a));
			}
		}
		catch (KeyStoreException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (Exception e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	public void encrypt(DeliveryChannel deliveryChannel, EbMSDocument message) throws EbMSProcessorException
	{
		try
		{
			val certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
			SecurityUtils.validateCertificate(trustStore, certificate, Instant.now());
			val encryptionAlgorithm = CPAUtils.getEncryptionAlgorithm(deliveryChannel);
			val attachments = new ArrayList<EbMSAttachment>();
			message.getAttachments().forEach(a -> attachments.add(encrypt(createDocument(), certificate, encryptionAlgorithm, a)));
			message.getAttachments().clear();
			message.getAttachments().addAll(attachments);
		}
		catch (TransformerFactoryConfigurationError | KeyStoreException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (Exception e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private XMLCipher createXmlCipher(String encryptionAlgorithm, SecretKey secretKey) throws XMLEncryptionException
	{
		val result = XMLCipher.getInstance(encryptionAlgorithm);
		result.init(XMLCipher.ENCRYPT_MODE, secretKey);
		return result;
	}

	private EbMSAttachment encrypt(Document document, X509Certificate certificate, String encryptionAlgorithm, EbMSAttachment attachment)
			throws ValidatorException
	{
		try
		{
			val secretKey = SecurityUtils.generateKey(encryptionAlgorithm);
			val xmlCipher = createXmlCipher(encryptionAlgorithm, secretKey);
			val encryptedKey = createEncryptedKey(document, certificate.getPublicKey(), secretKey);
			setEncryptedData(document, xmlCipher, encryptedKey, certificate, attachment);
			val encryptedData = xmlCipher.encryptData(document, null, attachment.getInputStream());
			val content = new CachedOutputStream();
			val transformer = DOMUtils.getTransformer();
			transformer.transform(new DOMSource(xmlCipher.martial(document, encryptedData)), new StreamResult(content));
			content.lockOutputStream();
			return EbMSAttachmentFactory.createCachedEbMSAttachment(attachment.getName(), attachment.getContentId(), "application/xml", content);
		}
		catch (NoSuchAlgorithmException | XMLEncryptionException | TransformerConfigurationException | TransformerFactoryConfigurationError e)
		{
			throw new ValidatorException(e);
		}
		catch (Exception e)
		{
			throw new ValidationException(e);
		}
	}

	private EncryptedKey createEncryptedKey(Document document, Key publicKey, SecretKey secretKey) throws XMLEncryptionException
	{
		val keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
		keyCipher.init(XMLCipher.WRAP_MODE, publicKey);
		return keyCipher.encryptKey(document, secretKey);
	}

	private void setEncryptedData(Document document, XMLCipher xmlCipher, EncryptedKey encryptedKey, X509Certificate certificate, EbMSAttachment attachment)
			throws XMLEncryptionException
	{
		val encryptedData = xmlCipher.getEncryptedData();
		val encryptedKeyInfo = new KeyInfo(document);
		encryptedKeyInfo.add(new KeyName(document, certificate.getSubjectDN().getName()));
		encryptedKey.setKeyInfo(encryptedKeyInfo);
		val keyInfo = new KeyInfo(document);
		keyInfo.add(encryptedKey);
		encryptedData.setKeyInfo(keyInfo);
		encryptedData.setId(attachment.getContentId());
		encryptedData.setMimeType(attachment.getContentType());
		encryptedData.setType(EncryptionConstants.TYPE_ELEMENT);
	}

	private Document createDocument() throws EbMSProcessorException
	{
		try
		{
			val builder = DOMUtils.getDocumentBuilder();
			return builder.parse(new InputSource(new StringReader("<root></root>")));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

}
