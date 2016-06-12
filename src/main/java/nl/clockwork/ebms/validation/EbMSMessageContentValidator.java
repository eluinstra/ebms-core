package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.Party;

public class EbMSMessageContentValidator
{
	protected EbMSMessageContextValidator messageContextValidator;
	protected MessageOrderValidator messageOrderValidator;
	protected PackageValidator packageValidator;

	public void validateMessage(EbMSMessageContent messageContent) throws ValidatorException
	{
		messageContextValidator.validate(messageContent.getContext());
		messageOrderValidator.generateSequenceNr(messageContent.getContext());
		packageValidator.validate(messageContent);
	}

	public void validatePing(String cpaId, Party fromParty, Party toParty) throws ValidationException
	{
		messageContextValidator.validate(cpaId,fromParty,toParty);
	}

	public void validateMessageStatus(String cpaId, Party fromParty, Party toParty) throws ValidationException
	{
		messageContextValidator.validate(cpaId,fromParty,toParty);
	}

	public void setMessageContextValidator(EbMSMessageContextValidator messageContextValidator)
	{
		this.messageContextValidator = messageContextValidator;
	}

	public void setMessageOrderValidator(MessageOrderValidator messageOrderValidator)
	{
		this.messageOrderValidator = messageOrderValidator;
	}

	public void setPackageValidator(PackageValidator packageValidator)
	{
		this.packageValidator = packageValidator;
	}
}
