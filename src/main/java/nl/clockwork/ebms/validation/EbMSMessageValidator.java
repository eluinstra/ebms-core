package nl.clockwork.ebms.validation;

import java.util.Date;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageDecrypter;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.CPAUtils;

public class EbMSMessageValidator
{
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;
	protected SSLSessionValidator sslCertificateValidator;
	protected CPAValidator cpaValidator;
	protected MessageHeaderValidator messageHeaderValidator;
	protected MessageOrderValidator messageOrderValidator;
	protected ManifestValidator manifestValidator;
	protected SignatureValidator signatureValidator;
	protected EbMSMessageDecrypter messageDecrypter;

	public void validateMessage(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(message))
			throw new DuplicateMessageException();
		cpaValidator.validate(message);
		messageHeaderValidator.validate(message,timestamp);
		sslCertificateValidator.validate(message);
		messageOrderValidator.validate(message);
		signatureValidator.validate(message);
		manifestValidator.validate(message);
		messageDecrypter.decrypt(message);
		signatureValidator.validateSignature(message);
	}

	public void validateMessageError(EbMSMessage requestMessage, EbMSMessage responseMessage, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage))
			throw new DuplicateMessageException();
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
		sslCertificateValidator.validate(responseMessage);
	}

	public void validateAcknowledgment(EbMSMessage requestMessage, EbMSMessage responseMessage, Date timestamp) throws ValidatorException
	{
		if (isDuplicateMessage(responseMessage))
			throw new DuplicateMessageException();
		messageHeaderValidator.validate(requestMessage,responseMessage);
		messageHeaderValidator.validate(responseMessage,timestamp);
		sslCertificateValidator.validate(responseMessage);
		signatureValidator.validate(requestMessage,responseMessage);
	}

	public void validateStatusRequest(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		sslCertificateValidator.validate(message);
	}

	public void validateStatusResponse(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		sslCertificateValidator.validate(message);
	}

	public void validatePing(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		sslCertificateValidator.validate(message);
	}

	public void validatePong(EbMSMessage message, Date timestamp) throws ValidatorException
	{
		messageHeaderValidator.validate(message,timestamp);
		sslCertificateValidator.validate(message);
	}

	public boolean isSyncReply(EbMSMessage message)
	{
		try
		{
			//return message.getSyncReply() != null;
			SyncReplyModeType syncReply = cpaManager.getSyncReply(message.getMessageHeader().getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(message.getMessageHeader().getService()),message.getMessageHeader().getAction());
			return syncReply != null && !syncReply.equals(SyncReplyModeType.NONE);
		}
		catch (Exception e)
		{
			return message.getSyncReply() != null;
		}
	}

	public boolean isDuplicateMessage(EbMSMessage message)
	{
		return /*message.getMessageHeader().getDuplicateElimination()!= null && */ebMSDAO.existsMessage(message.getMessageHeader().getMessageData().getMessageId());
	}
	
	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setSslCertificateValidator(SSLSessionValidator sslCertificateValidator)
	{
		this.sslCertificateValidator = sslCertificateValidator;
	}

	public void setCpaValidator(CPAValidator cpaValidator)
	{
		this.cpaValidator = cpaValidator;
	}

	public void setMessageHeaderValidator(MessageHeaderValidator messageHeaderValidator)
	{
		this.messageHeaderValidator = messageHeaderValidator;
	}

	public void setMessageOrderValidator(MessageOrderValidator messageOrderValidator)
	{
		this.messageOrderValidator = messageOrderValidator;
	}

	public void setManifestValidator(ManifestValidator manifestValidator)
	{
		this.manifestValidator = manifestValidator;
	}

	public void setSignatureValidator(SignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}

	public void setMessageDecrypter(EbMSMessageDecrypter messageDecrypter)
	{
		this.messageDecrypter = messageDecrypter;
	}
}
