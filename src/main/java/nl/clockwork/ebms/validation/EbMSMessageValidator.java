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
package nl.clockwork.ebms.validation;

import java.time.Instant;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSRequestMessage;
import nl.clockwork.ebms.util.StreamUtils;

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
		messageHeaderValidator.validate(message,timestamp);
		signatureValidator.validate(message);
		manifestValidator.validate(message);
		messageDecrypter.decrypt(message);
		signatureValidator.validateSignature(document,message);
	}

	public void validateMessageError(EbMSMessage requestMessage, EbMSMessageError responseMessage, Instant timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage.getMessageHeader()))
			throw new DuplicateMessageException();
		clientCertificateValidator.validate(responseMessage);
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
	}

	public void validateAcknowledgment(EbMSDocument responseDocument, EbMSMessage requestMessage, EbMSAcknowledgment responseMessage, Instant timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage.getMessageHeader()))
			throw new DuplicateMessageException();
		clientCertificateValidator.validate(responseMessage);
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
		signatureValidator.validate(responseDocument,requestMessage,responseMessage);
	}

	public void validate(EbMSBaseMessage message, Instant timestamp) throws ValidatorException
	{
		clientCertificateValidator.validate(message);
		messageHeaderValidator.validate(message,timestamp);
	}

	public boolean isSyncReply(EbMSRequestMessage message)
	{
		try
		{
			//return message.getSyncReply() != null;
			val messageHeader = message.getMessageHeader();
			val service = CPAUtils.toString(messageHeader.getService());
			val syncReply = cpaManager.getSyncReply(messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SyncReply",messageHeader.getCPAId(),messageHeader.getFrom().getPartyId(),messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (Exception e)
		{
			return message.getSyncReply() != null;
		}
	}

	public boolean isDuplicateMessage(MessageHeader messageHeader)
	{
		return /*messageHeader.getDuplicateElimination()!= null && */
				ebMSDAO.existsMessage(messageHeader.getMessageData().getMessageId());
	}
}
