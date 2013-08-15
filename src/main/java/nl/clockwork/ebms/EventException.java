package nl.clockwork.ebms;

public class EventException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public EventException()
	{
		super();
	}

	public EventException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public EventException(String message)
	{
		super(message);
	}

	public EventException(Throwable cause)
	{
		super(cause);
	}

}
