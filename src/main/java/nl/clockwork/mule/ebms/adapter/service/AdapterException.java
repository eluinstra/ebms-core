package nl.clockwork.mule.ebms.adapter.service;

import javax.xml.ws.WebFault;


@WebFault(name="AdapterException", targetNamespace="http://www.clockwork.nl/ebms/adapter/1.0")
public class AdapterException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public AdapterException()
	{
		super();
	}

	public AdapterException(String message, Throwable cause)
	{
		super(message,cause);
	}

	public AdapterException(String message)
	{
		super(message);
	}

	public AdapterException(Throwable cause)
	{
		super(cause);
	}
}
