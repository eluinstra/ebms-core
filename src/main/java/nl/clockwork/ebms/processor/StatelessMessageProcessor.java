package nl.clockwork.ebms.processor;

import java.time.Instant;

import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.validation.ClientCertificateValidator;
import nl.clockwork.ebms.validation.MessageHeaderValidator;
import nl.clockwork.ebms.validation.ValidatorException;

public abstract class StatelessMessageProcessor
{
	public void validate(EbMSBaseMessage message, Instant timestamp) throws ValidatorException
	{
		getClientCertificateValidator().validate(message);
		getMessageHeaderValidator().validate(message,timestamp);
	}

	public abstract ClientCertificateValidator getClientCertificateValidator();
	public abstract MessageHeaderValidator getMessageHeaderValidator();

}
