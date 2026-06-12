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
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPAUtils;
import nl.clockwork.ebms.common.message.EbMSAttachmentFactory;
import nl.clockwork.ebms.common.model.EbMSAttachment;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.common.util.ValidationException;
import nl.clockwork.ebms.common.util.ValidatorException;
import nl.clockwork.ebms.server.processor.EbMSProcessingException;
import nl.clockwork.ebms.server.processor.EbMSProcessorException;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

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
				SecurityUtils.validateCertificate(trustStore, certificate, Instant.now());
				val encryptionAlgorithm = nl.clockwork.ebms.common.cpa.CPAUtils.getEncryptionAlgorithm(deliveryChannel);
				message.getAttachments().replaceAll(a -> encrypt(certificate, encryptionAlgorithm, a));
			}
		}
		catch (KeyStoreException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (RuntimeException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	public void encrypt(DeliveryChannel deliveryChannel, EbMSDocument message) throws EbMSProcessorException
	{
		try
		{
			val certificate = CPAUtils.getX509Certificate(nl.clockwork.ebms.common.cpa.CPAUtils.getEncryptionCertificate(deliveryChannel));
			SecurityUtils.validateCertificate(trustStore, certificate, Instant.now());
			val encryptionAlgorithm = nl.clockwork.ebms.common.cpa.CPAUtils.getEncryptionAlgorithm(deliveryChannel);
			val attachments = new ArrayList<EbMSAttachment>();
			message.getAttachments().forEach(a -> attachments.add(encrypt(certificate, encryptionAlgorithm, a)));
			message.getAttachments().clear();
			message.getAttachments().addAll(attachments);
		}
		catch (KeyStoreException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (RuntimeException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private EbMSAttachment encrypt(X509Certificate certificate, String encryptionAlgorithm, EbMSAttachment attachment) throws ValidatorException
	{
		try (InputStream in = attachment.getInputStream())
		{
			val content = StreamingXmlEncrypter.encrypt(in, certificate, encryptionAlgorithm, attachment.getContentId(), attachment.getContentType());
			return EbMSAttachmentFactory.createCachedEbMSAttachment(attachment.getName(), attachment.getContentId(), "application/xml", content);
		}
		catch (GeneralSecurityException e)
		{
			throw new ValidatorException(e);
		}
		catch (IOException | RuntimeException e)
		{
			throw new ValidationException(e);
		}
	}
}
