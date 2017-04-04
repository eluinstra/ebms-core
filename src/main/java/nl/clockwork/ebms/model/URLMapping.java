package nl.clockwork.ebms.model;

import java.io.Serializable;

public class URLMapping implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String source;
	private String destination;

	public URLMapping()
	{
	}
	public URLMapping(String source, String destination)
	{
		this.source = source;
		this.destination = destination;
	}
	public String getSource()
	{
		return source;
	}
	public void setSource(String source)
	{
		this.source = source;
	}
	public String getDestination()
	{
		return destination;
	}
	public void setDestination(String destination)
	{
		this.destination = destination;
	}
}
