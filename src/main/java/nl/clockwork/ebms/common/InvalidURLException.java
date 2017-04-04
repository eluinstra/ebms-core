package nl.clockwork.ebms.common;

public class InvalidURLException extends Exception
{
	private static final long serialVersionUID = 1L;

	public InvalidURLException()
	{
		super();
	}

	public InvalidURLException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message,cause,enableSuppression,writableStackTrace);
	}

	public InvalidURLException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public InvalidURLException(String message)
	{
		super(message);
	}

	public InvalidURLException(Throwable cause)
	{
		super(cause);
	}
}
