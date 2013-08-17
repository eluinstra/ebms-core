package nl.clockwork.ebms.client;

import nl.clockwork.ebms.processor.EbMSProcessingException;

public class ReaderException extends EbMSProcessingException
{
	private static final long serialVersionUID = 1L;
	private int statusCode;

	public ReaderException(int statusCode)
	{
		this.statusCode = statusCode;
	}
	
	public ReaderException(int statusCode, String message)
	{
		super(message);
		this.statusCode = statusCode;
	}
	
	@Override
	public String getMessage()
	{
		return "StatusCode: " + statusCode + "\n" + super.getMessage();
	}

	public int getStatusCode()
	{
		return statusCode;
	}
}
