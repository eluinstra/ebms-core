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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.ThrowingConsumer;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

public class EbMSMessageDecrypter implements InitializingBean
{
	private CPAManager cpaManager;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStore keyStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		keyStore = KeyStoreManager.getKeyStore(keyStorePath,keyStorePassword);
	}

	public void decrypt(EbMSMessage message) throws ValidatorException
	{
		try
		{
			if (cpaManager.isConfidential(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction()))
			{
				DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getTo().getPartyId()),message.getMessageHeader().getTo().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction());
				X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				String alias = keyStore.getCertificateAlias(certificate);
				if (alias == null)
					throw new ValidationException("No certificate found with subject \"" + certificate.getSubjectDN().getName() + "\" in keystore \"" + keyStorePath + "\"");
				KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,alias,keyStorePassword);
				List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();
				message.getAttachments().stream().forEach(ThrowingConsumer.throwingConsumerWrapper(a -> attachments.add(decrypt(keyPair,a))));
				message.setAttachments(attachments);
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
	}

	private XMLCipher createXmlCipher(KeyPair keyPair) throws XMLEncryptionException, GeneralSecurityException
	{
		XMLCipher result = XMLCipher.getInstance();
		result.init(XMLCipher.DECRYPT_MODE,null);
		result.setKEK(keyPair.getPrivate());
		return result;
	}

	private EbMSAttachment decrypt(KeyPair keyPair, EbMSAttachment attachment) throws ValidatorException
	{
		try
		{
			Document document = DOMUtils.read((attachment.getInputStream()));
			if (document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).getLength() == 0)
				throw new EbMSProcessingException("Attachment " + attachment.getContentId() + " not encrypted!");
			Element encryptedDataElement = (Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);
			XMLCipher xmlCipher = createXmlCipher(keyPair);
			byte[] buffer = xmlCipher.decryptToByteArray(encryptedDataElement);
			String contentType = encryptedDataElement.getAttribute("MimeType");
			ByteArrayDataSource ds = new ByteArrayDataSource(new ByteArrayInputStream(buffer),contentType);
			ds.setName(attachment.getName());
			return new EbMSAttachment(ds,attachment.getContentId());
		}
		catch (ParserConfigurationException | GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
		catch (SAXException | IOException | EbMSProcessingException | XMLEncryptionException | IllegalArgumentException e)
		{
			throw new EbMSValidationException(EbMSMessageUtils.createError("cid:" + attachment.getContentId(),Constants.EbMSErrorCode.SECURITY_FAILURE,e.getMessage()));
		}
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
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
