package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;

public class PackageValidator
{
	@SuppressWarnings("unused")
	private CPAManager cpaManager;

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		
	}

	public void validate(EbMSMessageContent messageContent) throws ValidatorException
	{
		
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}
