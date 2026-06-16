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
package nl.clockwork.ebms.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import javax.xml.stream.XMLStreamException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPAUtils;
import nl.clockwork.ebms.common.message.EbMSAttachmentFactory;
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.protocol.EbMSErrorCode;
import nl.clockwork.ebms.common.util.EbMSValidationException;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.common.util.ValidationException;
import nl.clockwork.ebms.common.util.ValidatorException;
import nl.clockwork.ebms.server.processing.EbMSProcessingException;

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
			if (cpaManager.isSendingConfidential(
					messageHeader.getCPAId(),
					messageHeader.getFrom().getPartyId(),
					messageHeader.getFrom().getRole(),
					messageHeader.getService(),
					messageHeader.getAction()))
			{
				val toPartyId = messageHeader.getTo().getPartyId();
				val deliveryChannel = cpaManager
						.getReceiveDeliveryChannel(
								messageHeader.getCPAId(),
								toPartyId,
								messageHeader.getTo().getRole(),
								messageHeader.getService(),
								messageHeader.getAction())
						.orElseThrow(
								() -> StreamUtils.illegalStateException(
										"ReceiveDeliveryChannel",
										messageHeader.getCPAId(),
										toPartyId,
										messageHeader.getTo().getRole(),
										messageHeader.getService(),
										messageHeader.getAction()));
				val certificate = CPAUtils.getX509Certificate(nl.clockwork.ebms.common.cpa.CPAUtils.getEncryptionCertificate(deliveryChannel));
				if (certificate == null)
					throw new EbMSProcessingException(
							"No encryption certificate found for deliveryChannel \"" + deliveryChannel.getChannelId() + "\" in CPA \"" + messageHeader.getCPAId() + "\"");
				val alias = keyStore.getCertificateAlias(certificate);
				if (alias == null)
					throw new ValidationException(
							"No certificate found with subject \"" + certificate.getSubjectX500Principal().getName() + "\" in keystore \"" + keyStore + "\"");
				val keyPair = SecurityUtils.getKeyPair(keyStore, alias, keyStore.getKeyPassword());
				message.getAttachments().replaceAll(a -> decrypt(keyPair, a));
			}
		}
		catch (GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
	}

	private EbMSAttachment decrypt(KeyPair keyPair, EbMSAttachment attachment) throws ValidatorException
	{
		try (InputStream in = attachment.getInputStream())
		{
			val result = StreamingXmlDecrypter.decrypt(in, keyPair.getPrivate());
			return EbMSAttachmentFactory.createCachedEbMSAttachment(attachment.getName(), attachment.getContentId(), result.getMimeType(), result.getContent());
		}
		catch (ValidationException e)
		{
			throw new ValidationException("Attachment " + attachment.getContentId() + " not encrypted!");
		}
		catch (XMLStreamException | IOException | GeneralSecurityException | IllegalArgumentException e)
		{
			throw new EbMSValidationException(EbMSMessageUtils.createError("cid:" + attachment.getContentId(), EbMSErrorCode.SECURITY_FAILURE, e.getMessage()));
		}
	}
}
