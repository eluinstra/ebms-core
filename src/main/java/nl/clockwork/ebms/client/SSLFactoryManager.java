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
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;

public class SSLFactoryManager implements InitializingBean
{
	public class EbMSX509KeyManager implements X509KeyManager
	{
		private final String clientAlias;
		private final X509KeyManager standardKeyManager;

		public EbMSX509KeyManager(X509KeyManager standardKeyManager, String clientAlias)
		{
			this.clientAlias = clientAlias;
			this.standardKeyManager = standardKeyManager;
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket)
		{
			return standardKeyManager.chooseServerAlias(keyType,issuers,socket);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket)
		{
			return clientAlias == null ? standardKeyManager.chooseClientAlias(keyType,issuers,socket) : clientAlias;
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers)
		{
			return standardKeyManager.getServerAliases(keyType,issuers);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers)
		{
			return standardKeyManager.getClientAliases(keyType,issuers);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias)
		{
			return standardKeyManager.getCertificateChain(alias);
		}

		@Override
		public PrivateKey getPrivateKey(String alias)
		{
			return standardKeyManager.getPrivateKey(alias);
		}
	}

	public class SSLSocketFactoryWrapper extends SSLSocketFactory
	{
		private final SSLSocketFactory sslSocketFactory;
		private final SSLParameters sslParameters;

		public SSLSocketFactoryWrapper(SSLSocketFactory sslSocketFactory, SSLParameters sslParameters)
		{
			Objects.requireNonNull(sslSocketFactory);
			Objects.requireNonNull(sslParameters);
			this.sslSocketFactory = sslSocketFactory;
			this.sslParameters = sslParameters;
		}

		@Override
		public Socket createSocket() throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket();
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(s,consumed,autoClose);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(s,host,port,autoClose);
			socket.setSSLParameters(sslParameters);
			return socket;
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
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(host,port,localHost,localPort);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
		{
			SSLSocket socket = (SSLSocket)sslSocketFactory.createSocket(address,port,localAddress,localPort);
			socket.setSSLParameters(sslParameters);
			return socket;
		}
	}

	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStoreType trustStoreType;
	private String trustStorePath;
	private String trustStorePassword;
	private boolean verifyHostnames;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private String clientAlias;
	private SSLSocketFactory sslSocketFactory;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		KeyStore keyStore = KeyStoreManager.getKeyStore(keyStoreType,keyStorePath,keyStorePassword);
		KeyStore trustStore = KeyStoreManager.getKeyStore(trustStoreType,trustStorePath,trustStorePassword);

		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore,keyStorePassword.toCharArray());

		KeyManager[] keyManagers = kmf.getKeyManagers();
		for (int i = 0; i < keyManagers.length; i++)
			if (keyManagers[i] instanceof X509KeyManager)
				keyManagers[i] = new EbMSX509KeyManager((X509KeyManager)keyManagers[i],clientAlias);

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

		//SSLEngine engine = sslContext.createSSLEngine(hostname,port);
		SSLEngine engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);
		//engine.setSSLParameters(createSSLParameters());

		//sslSocketFactory = sslContext.getSocketFactory();
		sslSocketFactory = new SSLSocketFactoryWrapper(sslContext.getSocketFactory(),createSSLParameters());
	}

	private SSLParameters createSSLParameters()
	{
		SSLParameters result = new SSLParameters();
		if (enabledProtocols.length > 0)
			result.setProtocols(enabledProtocols);
		if (enabledProtocols.length > 0)
		{
			result.setCipherSuites(enabledCipherSuites);
			result.setUseCipherSuitesOrder(true);
		}
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

	public void setKeyStoreType(KeyStoreType keyStoreType)
	{
		this.keyStoreType = keyStoreType;
	}

	public void setKeyStorePath(String keyStorePath)
	{
		this.keyStorePath = keyStorePath;
	}

	public void setKeyStorePassword(String keyStorePassword)
	{
		this.keyStorePassword = keyStorePassword;
	}

	public void setTrustStoreType(KeyStoreType trustStoreType)
	{
		this.trustStoreType = trustStoreType;
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

	public void setEnabledProtocols(String[] enabledProtocols)
	{
		this.enabledProtocols = enabledProtocols;
	}

	public void setEnabledProtocols(String enabledProtocols)
	{
		this.enabledProtocols = StringUtils.split(enabledProtocols,",");
	}

	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}

	public void setEnabledCipherSuites(String enabledCipherSuites)
	{
		this.enabledCipherSuites = StringUtils.split(enabledCipherSuites,",");
	}

	public void setClientAlias(String clientAlias)
	{
		this.clientAlias = clientAlias;
	}

}
