package nl.clockwork.mule.ebms.service;

import javax.xml.ws.WebFault;


@WebFault(name="ebMSException", targetNamespace="http://www.clockwork.nl/ebms/v1")
public class EbMSException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public EbMSException()
	{
		super();
	}

	public EbMSException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public EbMSException(String message)
	{
		super(message);
	}

	public EbMSException(Throwable cause)
	{
		super(cause);
	}
}
