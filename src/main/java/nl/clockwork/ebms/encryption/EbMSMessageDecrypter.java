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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;
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
	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStore keyStore;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		keyStore = KeyStoreManager.getKeyStore(keyStoreType,keyStorePath,keyStorePassword);
	}

	public void decrypt(EbMSMessage message) throws ValidatorException
	{
		try
		{
			MessageHeader messageHeader = message.getMessageHeader();
			CacheablePartyId fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			String service = CPAUtils.toString(messageHeader.getService());
			if (cpaManager.isConfidential(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()))
			{
				CacheablePartyId toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
				DeliveryChannel deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
						.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
				X509Certificate certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				if (certificate == null)
					throw new EbMSProcessingException(
							"No encryption certificate found for deliveryChannel \"" + deliveryChannel.getChannelId() + "\" in CPA \"" + messageHeader.getCPAId() + "\"");
				String alias = keyStore.getCertificateAlias(certificate);
				if (alias == null)
					throw new ValidationException(
							"No certificate found with subject \"" + certificate.getSubjectDN().getName() + "\" in keystore \"" + keyStorePath + "\"");
				KeyPair keyPair = SecurityUtils.getKeyPair(keyStore,alias,keyStorePassword);
				List<EbMSAttachment> attachments = new ArrayList<>();
				message.getAttachments().forEach(a -> attachments.add(decrypt(keyPair,a)));
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
			Element encryptedDataElement =
					(Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);
			XMLCipher xmlCipher = createXmlCipher(keyPair);
			byte[] buffer = xmlCipher.decryptToByteArray(encryptedDataElement);
			String contentType = encryptedDataElement.getAttribute("MimeType");
			return EbMSAttachmentFactory.createCachedEbMSAttachment(attachment.getName(),attachment.getContentId(),contentType,new ByteArrayInputStream(buffer));
		}
		catch (ParserConfigurationException | GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
		catch (SAXException | IOException | EbMSProcessingException | XMLEncryptionException | IllegalArgumentException e)
		{
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("cid:" + attachment.getContentId(),Constants.EbMSErrorCode.SECURITY_FAILURE,e.getMessage()));
		}
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setKeyStoreType(KeyStoreType keyStoreType)
	{
		this.keyStoreType = keyStoreType;
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
