package nl.clockwork.ebms.validation;

public class ValidationException extends ValidatorException
{
	private static final long serialVersionUID = 1L;

	public ValidationException()
	{
		super();
	}

	public ValidationException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public ValidationException(String message)
	{
		super(message);
	}

	public ValidationException(Throwable cause)
	{
		super(cause);
	}

}
