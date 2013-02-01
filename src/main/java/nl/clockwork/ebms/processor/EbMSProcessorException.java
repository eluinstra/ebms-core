package nl.clockwork.ebms.processor;

public class EbMSProcessorException extends Exception
{
	private static final long serialVersionUID = 1L;

	public EbMSProcessorException()
	{
		super();
	}

	public EbMSProcessorException(String arg0, Throwable arg1)
	{
		super(arg0,arg1);
	}

	public EbMSProcessorException(String arg0)
	{
		super(arg0);
	}

	public EbMSProcessorException(Throwable arg0)
	{
		super(arg0);
	}

}
