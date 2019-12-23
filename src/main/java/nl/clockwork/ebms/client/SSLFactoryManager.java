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
package nl.clockwork.ebms.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import nl.clockwork.ebms.common.KeyStoreManager;

public class SSLFactoryManager
{
	public class SSLSocketFactoryWrapper extends SSLSocketFactory
	{
		private SSLSocketFactory sslSocketFactory;

		public SSLSocketFactoryWrapper(SSLSocketFactory sslSocketFactory)
		{
			this.sslSocketFactory = sslSocketFactory;
		}

		@Override
		public Socket createSocket() throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket();
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(s,consumed,autoClose);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(s,host,port,autoClose);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public String[] getDefaultCipherSuites()
		{
			return sslSocketFactory.getDefaultCipherSuites();//enabledCipherSuites;
		}

		@Override
		public String[] getSupportedCipherSuites()
		{
			return sslSocketFactory.getSupportedCipherSuites();
		}

		@Override
		public Socket createSocket(String host, int port) throws IOException, UnknownHostException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port,localHost,localPort);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(address,port,localAddress,localPort);
			socket.setEnabledProtocols(enabledProtocols);
			socket.setEnabledCipherSuites(enabledCipherSuites);
//			socket.setSSLParameters(createSSLParameters());
			return socket;
		}
	}

	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
	private boolean verifyHostnames;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private SSLSocketFactory sslSocketFactory;

	public void afterPropertiesSet() throws Exception
	{
		KeyStore keyStore = KeyStoreManager.getKeyStore(keyStorePath,keyStorePassword);
		KeyStore trustStore = KeyStoreManager.getKeyStore(trustStorePath,trustStorePassword);

		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore,keyStorePassword.toCharArray());

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

		//SSLEngine engine = sslContext.createSSLEngine(hostname,port);
		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);
		if (enabledProtocols.length > 0)
			engine.setEnabledProtocols(enabledProtocols);
		if (enabledCipherSuites.length > 0)
			engine.setEnabledCipherSuites(enabledCipherSuites);
//		engine.setSSLParameters(createSSLParameters());

		sslSocketFactory = sslContext.getSocketFactory();
//		sslSocketFactory = new SSLSocketFactoryWrapper(sslContext.getSocketFactory());
	}

	@SuppressWarnings("unused")
	private SSLParameters createSSLParameters()
	{
		SSLParameters result = new SSLParameters();
		result.setProtocols(enabledProtocols);
		result.setCipherSuites(enabledCipherSuites);
		result.setUseCipherSuitesOrder(true);
		return result;
	}

	public HostnameVerifier getHostnameVerifier(HttpsURLConnection connection)
	{
		return verifyHostnames ? HttpsURLConnection.getDefaultHostnameVerifier() : (h,s) -> true;
	}
	
	public SSLSocketFactory getSslSocketFactory()
	{
		return sslSocketFactory;
	}

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}

	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}

	public void setTrustStorePath(String trustStorePath)
	{
		this.trustStorePath = trustStorePath;
	}

	public void setTrustStorePassword(String trustStorePassword)
	{
		this.trustStorePassword = trustStorePassword;
	}
	
	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
	}

	public String[] getEnabledProtocols()
	{
		return enabledProtocols;
	}

	public String[] getEnabledCipherSuites()
	{
		return enabledCipherSuites;
	}
}
