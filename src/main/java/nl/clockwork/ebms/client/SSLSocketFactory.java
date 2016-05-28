package nl.clockwork.ebms.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;

public class SSLSocketFactory extends javax.net.ssl.SSLSocketFactory
{
	private javax.net.ssl.SSLSocketFactory sslSocketFactory;

	public SSLSocketFactory(javax.net.ssl.SSLSocketFactory sslSocketFactory)
	{
		this.sslSocketFactory = sslSocketFactory;
	}

	@Override
	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
	{
		SSLSocket result = (SSLSocket)sslSocketFactory.createSocket(s,host,port,autoClose);
		result.addHandshakeCompletedListener(new SSLCertificateListener());
		return result;
	}

	@Override
	public String[] getDefaultCipherSuites()
	{
		return sslSocketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites()
	{
		return sslSocketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException
	{
		SSLSocket result = (SSLSocket)sslSocketFactory.createSocket(host,port);
		result.addHandshakeCompletedListener(new SSLCertificateListener());
		return result;
	}

	@Override
	public Socket createSocket(InetAddress host, int port) throws IOException
	{
		SSLSocket result = (SSLSocket)sslSocketFactory.createSocket(host,port);
		result.addHandshakeCompletedListener(new SSLCertificateListener());
		return result;
	}

	@Override
	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException
	{
		SSLSocket result = (SSLSocket)sslSocketFactory.createSocket(host,port,localHost,localPort);
		result.addHandshakeCompletedListener(new SSLCertificateListener());
		return result;
	}

	@Override
	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
	{
		SSLSocket result = (SSLSocket)sslSocketFactory.createSocket(address,port,localAddress,localPort);
		result.addHandshakeCompletedListener(new SSLCertificateListener());
		return result;
	}

}
