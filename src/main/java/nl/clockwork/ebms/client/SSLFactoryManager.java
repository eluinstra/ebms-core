package nl.clockwork.ebms.client;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import nl.clockwork.common.util.SecurityUtils;

public class SSLFactoryManager
{
	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;
  public String[] allowedCipherSuites = new String[]{};
	private boolean requireClientAuthentication;
	private boolean verifyHostnames;
	private SSLSocketFactory sslFactory;

	public void init() throws GeneralSecurityException, IOException
	{
		if (!verifyHostnames)
			setDefaultHostnameVerifier();

		KeyStore keyStore = SecurityUtils.loadKeyStore(keyStorePath,keyStorePassword);
		KeyStore trustStore = SecurityUtils.loadKeyStore(trustStorePath,trustStorePassword);

		//KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(keyStore,keyStorePassword.toCharArray());

		//TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(trustStore);

		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);

		//SSLEngine engine = sslContext.createSSLEngine(hostname, port);
		SSLEngine engine = sslContext.createSSLEngine();
		if (allowedCipherSuites.length > 0)
			engine.setEnabledCipherSuites(allowedCipherSuites);

		engine.setUseClientMode(requireClientAuthentication);

		sslFactory = sslContext.getSocketFactory();
	}

	private void setDefaultHostnameVerifier()
	{
		javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
			new javax.net.ssl.HostnameVerifier()
			{
				public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession)
				{
					return true;
				}
			}
		);
	}

	public SSLSocketFactory getSslFactory()
	{
		return sslFactory;
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
	
	public void setAllowedCipherSuites(String[] allowedCipherSuites)
	{
		this.allowedCipherSuites = allowedCipherSuites;
	}

	public void setRequireClientAuthentication(boolean requireClientAuthentication)
	{
		this.requireClientAuthentication = requireClientAuthentication;
	}
	
	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
	}
}
