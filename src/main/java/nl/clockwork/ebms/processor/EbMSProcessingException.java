package nl.clockwork.ebms.processor;

public class EbMSProcessingException extends Exception
{
	private static final long serialVersionUID = 1L;

	public EbMSProcessingException()
	{
		super();
	}

	public EbMSProcessingException(String arg0, Throwable arg1)
	{
		super(arg0,arg1);
	}

	public EbMSProcessingException(String arg0)
	{
		super(arg0);
	}

	public EbMSProcessingException(Throwable arg0)
	{
		super(arg0);
	}

}
