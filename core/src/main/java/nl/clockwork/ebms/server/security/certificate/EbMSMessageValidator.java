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
package nl.clockwork.ebms.server.security.certificate;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.dao.EbMSDAO;
import nl.clockwork.ebms.common.model.EbMSAcknowledgment;
import nl.clockwork.ebms.common.model.EbMSBaseMessage;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.model.EbMSMessage;
import nl.clockwork.ebms.common.model.EbMSMessageError;
import nl.clockwork.ebms.common.model.EbMSRequestMessage;
import nl.clockwork.ebms.common.security.EbMSMessageDecrypter;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.common.util.ValidatorException;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageValidator
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	CPAValidator cpaValidator;
	@NonNull
	MessageHeaderValidator messageHeaderValidator;
	@NonNull
	ManifestValidator manifestValidator;
	@NonNull
	SignatureValidator signatureValidator;
	@NonNull
	EbMSMessageDecrypter messageDecrypter;
	@NonNull
	ClientCertificateValidator clientCertificateValidator;

	public void validateAndDecryptMessage(EbMSDocument document, EbMSMessage message, Instant timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(message.getMessageHeader()))
			throw new DuplicateMessageException();
		clientCertificateValidator.validate(message);
		cpaValidator.validate(message);
		messageHeaderValidator.validate(message, timestamp);
		signatureValidator.validate(message);
		manifestValidator.validate(message);
		messageDecrypter.decrypt(message);
		signatureValidator.validateSignature(document, message);
	}

	public void validateMessageError(EbMSMessage requestMessage, EbMSMessageError responseMessage) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage.getMessageHeader()))
			throw new DuplicateMessageException();
		clientCertificateValidator.validate(responseMessage);
		messageHeaderValidator.validate(requestMessage, responseMessage);
		messageHeaderValidator.validate(responseMessage);
	}

	public void validateAcknowledgment(EbMSDocument responseDocument, EbMSMessage requestMessage, EbMSAcknowledgment responseMessage) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage.getMessageHeader()))
			throw new DuplicateMessageException();
		clientCertificateValidator.validate(responseMessage);
		messageHeaderValidator.validate(requestMessage, responseMessage);
		messageHeaderValidator.validate(responseMessage);
		signatureValidator.validate(responseDocument, requestMessage, responseMessage);
	}

	public void validate(EbMSBaseMessage message) throws ValidatorException
	{
		clientCertificateValidator.validate(message);
		messageHeaderValidator.validate(message);
	}

	public boolean isSyncReply(EbMSRequestMessage message)
	{
		try
		{
			// return message.getSyncReply() != null;
			val messageHeader = message.getMessageHeader();
			val syncReply = cpaManager
					.getSendSyncReply(
							messageHeader.getCPAId(),
							messageHeader.getFrom().getPartyId(),
							messageHeader.getFrom().getRole(),
							messageHeader.getService(),
							messageHeader.getAction())
					.orElseThrow(
							() -> StreamUtils.illegalStateException(
									"SyncReply",
									messageHeader.getCPAId(),
									messageHeader.getFrom().getPartyId(),
									messageHeader.getFrom().getRole(),
									messageHeader.getService(),
									messageHeader.getAction()));
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (RuntimeException e)
		{
			return message.getSyncReply() != null;
		}
	}

	public boolean isDuplicateMessage(MessageHeader messageHeader)
	{
		return ebMSDAO.existsMessage(messageHeader.getMessageData().getMessageId());
	}
}
