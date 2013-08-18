package nl.clockwork.ebms.client;

import nl.clockwork.ebms.processor.EbMSProcessingException;

public class EbMSResponseException extends EbMSProcessingException
{
	private static final long serialVersionUID = 1L;
	private int statusCode;

	public EbMSResponseException(int statusCode)
	{
		this.statusCode = statusCode;
	}
	
	public EbMSResponseException(int statusCode, String message)
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
