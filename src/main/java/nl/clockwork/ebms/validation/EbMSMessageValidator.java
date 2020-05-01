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

import java.util.Date;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;

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

	public void validateMessage(EbMSDocument document, EbMSMessage message, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(message))
			throw new DuplicateMessageException();
		cpaValidator.validate(message);
		messageHeaderValidator.validate(message,timestamp);
		clientCertificateValidator.validate(message);
		signatureValidator.validate(message);
		manifestValidator.validate(message);
		messageDecrypter.decrypt(message);
		signatureValidator.validateSignature(document,message);
	}

	public void validateMessageError(EbMSMessage requestMessage, EbMSMessage responseMessage, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage))
			throw new DuplicateMessageException();
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
		clientCertificateValidator.validate(responseMessage);
	}

	public void validateAcknowledgment(EbMSDocument responseDocument, EbMSMessage requestMessage, EbMSMessage responseMessage, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage))
			throw new DuplicateMessageException();
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
		clientCertificateValidator.validate(responseMessage);
		signatureValidator.validate(responseDocument,requestMessage,responseMessage);
	}

	public void validateStatusRequest(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		clientCertificateValidator.validate(message);
	}

	public void validateStatusResponse(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		clientCertificateValidator.validate(message);
	}

	public void validatePing(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		clientCertificateValidator.validate(message);
	}

	public void validatePong(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		clientCertificateValidator.validate(message);
	}

	public boolean isSyncReply(EbMSMessage message)
	{
		try
		{
			//return message.getSyncReply() != null;
			val messageHeader = message.getMessageHeader();
			val fromPartyId = new CacheablePartyId(messageHeader.getFrom().getPartyId());
			val service = CPAUtils.toString(messageHeader.getService());
			val syncReply = cpaManager.getSyncReply(messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction())
					.orElseThrow(() -> StreamUtils.illegalStateException("SyncReply",messageHeader.getCPAId(),fromPartyId,messageHeader.getFrom().getRole(),service,messageHeader.getAction()));
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (Exception e)
		{
			return message.getSyncReply() != null;
		}
	}

	public boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */
				ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
	}
}
