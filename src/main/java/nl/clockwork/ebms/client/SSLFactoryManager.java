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

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import nl.clockwork.ebms.common.util.SecurityUtils;
import nl.clockwork.ebms.validation.SSLSessionManager;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;

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
			//socket.getInetAddress().getHostName(); socket.getPort();
			return clientAlias;
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

	public static class SSLCertificateListener implements HandshakeCompletedListener
	{
		public void handshakeCompleted(HandshakeCompletedEvent event)
		{
			nl.clockwork.ebms.model.SSLSession sslSession = new nl.clockwork.ebms.model.SSLSession();
			sslSession.setCipherSuite(event.getSession().getCipherSuite());
			sslSession.setProtocol(event.getSession().getProtocol());
			sslSession.setLocalCertificates(event.getSession().getLocalCertificates());
			sslSession.setPeerHost(event.getSession().getPeerHost());
			sslSession.setPeerPort(event.getSession().getPeerPort());
			try
			{
				sslSession.setPeerCertificates(event.getSession().getPeerCertificates());
				sslSession.setPeerCertificateChain(event.getSession().getPeerCertificateChain());
			}
			catch (SSLPeerUnverifiedException e)
			{
			}
			SSLSessionManager.setSSLSession(sslSession);
		}
	}

	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
	private boolean verifyHostnames;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean requireClientAuthentication;
	private String clientAlias;
	private SSLSocketFactory sslSocketFactory;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		KeyStore keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
		KeyStore trustStore = SecurityUtils.loadKeyStore(trustStorePath,trustStorePassword);

		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore,keyStorePassword.toCharArray());

		KeyManager[] keyManagers = kmf.getKeyManagers();
		if (!StringUtils.isEmpty(clientAlias))
			for (int i = 0; i < keyManagers.length; i++)
				if (keyManagers[i] instanceof X509KeyManager)
					keyManagers[i] = new EbMSX509KeyManager((X509KeyManager)keyManagers[i],clientAlias);

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagers,tmf.getTrustManagers(),null);

		//SSLEngine engine = sslContext.createSSLEngine(hostname,port);
		SSLEngine engine = sslContext.createSSLEngine();
		if (enabledProtocols.length > 0)
			engine.setEnabledProtocols(enabledProtocols);
		if (enabledCipherSuites.length > 0)
			engine.setEnabledCipherSuites(enabledCipherSuites);
		engine.setNeedClientAuth(requireClientAuthentication);

		sslSocketFactory = sslContext.getSocketFactory();
	}

	public HostnameVerifier getHostnameVerifier(HttpsURLConnection connection)
	{
		return verifyHostnames ? HttpsURLConnection.getDefaultHostnameVerifier() : new HostnameVerifier()
		{
			@Override
			public boolean verify(String hostname, SSLSession sslSession)
			{
				return true;
			}
		};
	}
	
	@SuppressWarnings({"deprecation","restriction"})
	public com.sun.net.ssl.HostnameVerifier getHostnameVerifier(com.sun.net.ssl.HttpsURLConnection connection)
	{
		return verifyHostnames ? com.sun.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier() : new com.sun.net.ssl.HostnameVerifier()
		{
			@Override
			public boolean verify(String urlHostname, String certHostname)
			{
				return true;
			}
		};
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

	public void setEnabledProtocols(String[] enabledProtocols)
	{
		this.enabledProtocols = enabledProtocols;
	}

	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}

	public void setRequireClientAuthentication(boolean requireClientAuthentication)
	{
		this.requireClientAuthentication = requireClientAuthentication;
	}
	
	public void setClientAlias(String clientAlias)
	{
		this.clientAlias = clientAlias;
	}
}
