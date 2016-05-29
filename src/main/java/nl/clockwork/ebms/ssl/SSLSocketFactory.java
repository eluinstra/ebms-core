/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.ssl;

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
