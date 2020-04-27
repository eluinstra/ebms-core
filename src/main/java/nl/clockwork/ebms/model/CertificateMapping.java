package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.security.cert.X509Certificate;

public class CertificateMapping implements Serializable
{
	private static final long serialVersionUID = 1L;
	private X509Certificate source;
	private X509Certificate destination;

	public CertificateMapping()
	{
	}
	public CertificateMapping(X509Certificate source, X509Certificate destination)
	{
		this.source = source;
		this.destination = destination;
	}
	public X509Certificate getSource()
	{
		return source;
	}
	public void setSource(X509Certificate source)
	{
		this.source = source;
	}
	public X509Certificate getDestination()
	{
		return destination;
	}
	public void setDestination(X509Certificate destination)
	{
		this.destination = destination;
	}
}
