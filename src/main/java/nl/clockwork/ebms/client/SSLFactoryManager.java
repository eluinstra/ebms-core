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
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.EbMSTrustStore;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SSLFactoryManager
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	public class EbMSX509KeyManager implements X509KeyManager
	{
		@NonNull
		X509KeyManager standardKeyManager;
		String clientAlias;

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

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	public class SSLSocketFactoryWrapper extends SSLSocketFactory
	{
		@NonNull
		SSLSocketFactory sslSocketFactory;
		@NonNull
		SSLParameters sslParameters;

		@Override
		public Socket createSocket() throws IOException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket();
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket(s,consumed,autoClose);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket(s,host,port,autoClose);
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
			val socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress host, int port) throws IOException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket(host,port);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket(host,port,localHost,localPort);
			socket.setSSLParameters(sslParameters);
			return socket;
		}

		@Override
		public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
		{
			val socket = (SSLSocket)sslSocketFactory.createSocket(address,port,localAddress,localPort);
			socket.setSSLParameters(sslParameters);
			return socket;
		}
	}

	@NonNull
	EbMSKeyStore keyStore;
	@NonNull
	EbMSTrustStore trustStore;
	boolean verifyHostnames;
	@NonNull
	String[] enabledProtocols;
	@NonNull
	String[] enabledCipherSuites;
	String clientAlias;
	@Getter
	SSLSocketFactory sslSocketFactory;

	@Builder
	public SSLFactoryManager(
			@NonNull EbMSKeyStore keyStore,
			@NonNull EbMSTrustStore trustStore,
			boolean verifyHostnames,
			String[] enabledProtocols,
			String[] enabledCipherSuites,
			String clientAlias) throws Exception
	{
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.verifyHostnames = verifyHostnames;
		this.enabledProtocols = enabledProtocols == null ? new String[]{} : enabledProtocols;
		this.enabledCipherSuites = enabledCipherSuites == null ? new String[]{} : enabledCipherSuites;
		this.clientAlias = clientAlias;
		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		val kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore.getKeyStore(),keyStore.getKeyPassword().toCharArray());

		val keyManagers = kmf.getKeyManagers();
		for (var i = 0; i < keyManagers.length; i++)
			if (keyManagers[i] instanceof X509KeyManager)
				keyManagers[i] = new EbMSX509KeyManager((X509KeyManager)keyManagers[i],clientAlias);

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		val tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore.getKeyStore());

		val sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

		//val engine = sslContext.createSSLEngine(hostname,port);
		val engine = sslContext.createSSLEngine();
		engine.setUseClientMode(true);
		//engine.setSSLParameters(createSSLParameters());

		//sslSocketFactory = sslContext.getSocketFactory();
		sslSocketFactory = new SSLSocketFactoryWrapper(sslContext.getSocketFactory(),createSSLParameters());
	}

	
	private SSLParameters createSSLParameters()
	{
		val result = new SSLParameters();
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
	
}
