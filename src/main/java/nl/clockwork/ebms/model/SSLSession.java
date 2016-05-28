package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.security.cert.Certificate;

import javax.security.cert.X509Certificate;

public class SSLSession implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String cipherSuite;
	private String protocol;
	private Certificate[] localCertificates;
	private String peerHost;
	private int peerPort;
	private Certificate[] peerCertificates;
	private X509Certificate[] peerCertificateChain;

	public SSLSession()
	{
	}
	public SSLSession(String cipherSuite, String protocol, Certificate[] localCertificates, String peerHost, int peerPort, Certificate[] peerCertificates, X509Certificate[] peerCertificateChain)
	{
		this.cipherSuite = cipherSuite;
		this.protocol = protocol;
		this.localCertificates = localCertificates;
		this.peerHost = peerHost;
		this.peerPort = peerPort;
		this.peerCertificates = peerCertificates;
		this.peerCertificateChain = peerCertificateChain;
	}
	public String getCipherSuite()
	{
		return cipherSuite;
	}
	public void setCipherSuite(String cipherSuite)
	{
		this.cipherSuite = cipherSuite;
	}
	public String getProtocol()
	{
		return protocol;
	}
	public void setProtocol(String protocol)
	{
		this.protocol = protocol;
	}
	public Certificate[] getLocalCertificates()
	{
		return localCertificates;
	}
	public void setLocalCertificates(Certificate[] localCertificates)
	{
		this.localCertificates = localCertificates;
	}
	public String getPeerHost()
	{
		return peerHost;
	}
	public void setPeerHost(String peerHost)
	{
		this.peerHost = peerHost;
	}
	public int getPeerPort()
	{
		return peerPort;
	}
	public void setPeerPort(int peerPort)
	{
		this.peerPort = peerPort;
	}
	public Certificate[] getPeerCertificates()
	{
		return peerCertificates;
	}
	public void setPeerCertificates(Certificate[] peerCertificates)
	{
		this.peerCertificates = peerCertificates;
	}
	public X509Certificate[] getPeerCertificateChain()
	{
		return peerCertificateChain;
	}
	public void setPeerCertificateChain(X509Certificate[] peerCertificateChain)
	{
		this.peerCertificateChain = peerCertificateChain;
	}
	public static long getSerialversionuid()
	{
		return serialVersionUID;
	}
}