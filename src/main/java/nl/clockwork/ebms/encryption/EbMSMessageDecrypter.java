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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.utils.EncryptionConstants;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.util.SecurityUtils;
import nl.clockwork.ebms.util.StreamUtils;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageDecrypter
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSKeyStore keyStore;

	public void decrypt(EbMSMessage message) throws ValidatorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			if (cpaManager.isSendingConfidential(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()))
			{
				val toPartyId = messageHeader.getTo().getPartyId();
				val deliveryChannel = cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
						.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
				val certificate = CPAUtils.getX509Certificate(CPAUtils.getEncryptionCertificate(deliveryChannel));
				if (certificate == null)
					throw new EbMSProcessingException(
							"No encryption certificate found for deliveryChannel \"" + deliveryChannel.getChannelId() + "\" in CPA \"" + messageHeader.getCPAId() + "\"");
				val alias = keyStore.getCertificateAlias(certificate);
				if (alias == null)
					throw new ValidationException(
							"No certificate found with subject \"" + certificate.getSubjectDN().getName() + "\" in keystore \"" + keyStore + "\"");
				val keyPair = SecurityUtils.getKeyPair(keyStore,alias,keyStore.getKeyPassword());
				message.getAttachments().replaceAll(a -> decrypt(keyPair,a));
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
	}

	private XMLCipher createXmlCipher(KeyPair keyPair) throws XMLEncryptionException, GeneralSecurityException
	{
		val result = XMLCipher.getInstance();
		result.init(XMLCipher.DECRYPT_MODE,null);
		result.setKEK(keyPair.getPrivate());
		return result;
	}

	private EbMSAttachment decrypt(KeyPair keyPair, EbMSAttachment attachment) throws ValidatorException
	{
		try
		{
			val document = DOMUtils.read((attachment.getInputStream()));
			if (document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).getLength() == 0)
				throw new ValidationException("Attachment " + attachment.getContentId() + " not encrypted!");
			val encryptedDataElement =
					(Element)document.getElementsByTagNameNS(EncryptionConstants.EncryptionSpecNS,EncryptionConstants._TAG_ENCRYPTEDDATA).item(0);
			val xmlCipher = createXmlCipher(keyPair);
			val buffer = xmlCipher.decryptToByteArray(encryptedDataElement);
			val contentType = encryptedDataElement.getAttribute("MimeType");
			return EbMSAttachmentFactory.createCachedEbMSAttachment(attachment.getName(),attachment.getContentId(),contentType,new ByteArrayInputStream(buffer));
		}
		catch (ParserConfigurationException | GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
		catch (SAXException | IOException | XMLEncryptionException | IllegalArgumentException e)
		{
			throw new EbMSValidationException(
					EbMSMessageUtils.createError("cid:" + attachment.getContentId(),EbMSErrorCode.SECURITY_FAILURE,e.getMessage()));
		}
	}
}
