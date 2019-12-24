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
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.common.KeyStoreManager;
import nl.clockwork.ebms.common.KeyStoreManager.KeyStoreType;

public class SSLFactoryManager implements InitializingBean
{
	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private KeyStoreType trustStoreType;
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
		KeyStore keyStore = KeyStoreManager.getKeyStore(keyStoreType,keyStorePath,keyStorePassword);
		KeyStore trustStore = KeyStoreManager.getKeyStore(trustStoreType,trustStorePath,trustStorePassword);

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
		engine.setSSLParameters(createSSLParameters());
		engine.setNeedClientAuth(requireClientAuthentication);

		sslSocketFactory = sslContext.getSocketFactory();
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
		result.setNeedClientAuth(requireClientAuthentication);
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

	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}

	public void setRequireClientAuthentication(boolean requireClientAuthentication)
	{
		this.requireClientAuthentication = requireClientAuthentication;
	}
	
}
