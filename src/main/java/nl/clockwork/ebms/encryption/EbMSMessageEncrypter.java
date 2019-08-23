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
package nl.clockwork.ebms.encryption;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.SecretKey;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.keys.content.KeyName;
import org.apache.xml.security.utils.EncryptionConstants;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.ThrowingConsumer;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

public class EbMSMessageEncrypter implements InitializingBean
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

	public void encrypt(EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = message.getMessageHeader();
			CacheablePartyId fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			String service = CPAUtils.toString(messageHeader.getService());
			if (cpaManager.isConfidential(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()))
			{
				CacheablePartyId toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
				DeliveryChannel deliveryChannel =
						cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
						.orElseThrow(() -> 
						StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
				X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				validateCertificate(trustStore,certificate);
				String encryptionAlgorithm = CPAUtils.getEncryptionAlgorithm(deliveryChannel);
				List<EbMSAttachment> attachments = new ArrayList<>();
				message.getAttachments().forEach(ThrowingConsumer.throwingConsumerWrapper(a ->
						attachments.add(encrypt(createDocument(),certificate,encryptionAlgorithm,a))));
				message.setAttachments(attachments);
			}
		}
		catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | XMLEncryptionException | FileNotFoundException e)
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
			X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
			validateCertificate(trustStore,certificate);
			String encryptionAlgorithm = CPAUtils.getEncryptionAlgorithm(deliveryChannel);
			List<EbMSAttachment> attachments = new ArrayList<>();
			message.getAttachments().forEach(ThrowingConsumer.throwingConsumerWrapper(a ->
					attachments.add(encrypt(createDocument(),certificate,encryptionAlgorithm,a))));
			message.getAttachments().clear();
			message.getAttachments().addAll(attachments);
		}
		catch (TransformerFactoryConfigurationError | CertificateException | KeyStoreException | ValidatorException e)
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
		XMLCipher result = XMLCipher.getInstance(encryptionAlgorithm);
		result.init(XMLCipher.ENCRYPT_MODE,secretKey);
		return result;
	}

	private void validateCertificate(KeyStore trustStore, X509Certificate certificate) throws KeyStoreException, ValidationException
	{
		try
		{
			certificate.checkValidity(new Date());
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
		catch (CertificateExpiredException | CertificateNotYetValidException e)
		{
			throw new ValidationException(e);
		}
	}

	private EbMSAttachment encrypt(Document document, X509Certificate certificate, String encryptionAlgorithm, EbMSAttachment attachment) throws ValidatorException
	{
		try
		{
			SecretKey secretKey = SecurityUtils.generateKey(encryptionAlgorithm);
			XMLCipher xmlCipher = createXmlCipher(encryptionAlgorithm,secretKey);
			EncryptedKey encryptedKey = createEncryptedKey(document,certificate.getPublicKey(),secretKey);
			setEncryptedData(document,xmlCipher,encryptedKey,certificate,attachment);
			EncryptedData encryptedData = xmlCipher.encryptData(document,null,attachment.getInputStream());
			StringWriter buffer = new StringWriter();
			createTransformer().transform(new DOMSource(xmlCipher.martial(document,encryptedData)),new StreamResult(buffer));
			ByteArrayDataSource ds = new ByteArrayDataSource(buffer.toString().getBytes("UTF-8"),"application/xml");
			ds.setName(attachment.getName());
			return new EbMSAttachment(ds,attachment.getContentId());
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
		XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
		keyCipher.init(XMLCipher.WRAP_MODE,publicKey);
		return keyCipher.encryptKey(document,secretKey);
	}

	private void setEncryptedData(Document document, XMLCipher xmlCipher, EncryptedKey encryptedKey, X509Certificate certificate, EbMSAttachment attachment) throws XMLEncryptionException
	{
		EncryptedData encryptedData = xmlCipher.getEncryptedData();
		KeyInfo encryptedKeyInfo = new KeyInfo(document);
		encryptedKeyInfo.add(new KeyName(document,certificate.getSubjectDN().getName()));
		encryptedKey.setKeyInfo(encryptedKeyInfo);
		KeyInfo keyInfo = new KeyInfo(document);
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
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			DocumentBuilder builder = dbFactory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader("<root></root>")));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private Transformer createTransformer() throws TransformerFactoryConfigurationError, TransformerConfigurationException
	{
		TransformerFactory transormerFactory = TransformerFactory.newInstance();
		Transformer transformer = transormerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		return transformer;
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
