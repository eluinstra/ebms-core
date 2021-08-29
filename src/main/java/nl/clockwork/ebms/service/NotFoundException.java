package nl.clockwork.ebms.service;

public class NotFoundException extends EbMSMessageServiceException
{
	private static final long serialVersionUID = 1L;

	public NotFoundException()
	{
		super("Not found");
	}

	public NotFoundException(String message)
	{
		super(message);
	}

}
