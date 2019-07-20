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

import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import nl.clockwork.ebms.common.KeyStoreManager;

import org.springframework.beans.factory.InitializingBean;

public class SSLFactoryManager implements InitializingBean
{
	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
	private boolean verifyHostnames;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean requireClientAuthentication;
	private SSLSocketFactory sslSocketFactory;

	@Override
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
	
}
