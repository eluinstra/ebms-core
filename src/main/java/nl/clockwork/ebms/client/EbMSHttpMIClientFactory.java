package nl.clockwork.ebms.client;

import nl.clockwork.ebms.client.EbMSHttpClientFactory.EbMSHttpClientType;

public class EbMSHttpMIClientFactory
{
	private EbMSHttpClientType type;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;
	private String keyStorePath;
	private String keyStorePassword;
	private String trustStorePath;
	private String trustStorePassword;

	public EbMSClient createEbMSClient(String clientAlias)
	{
		try
		{
			EbMSHttpClientFactory ebMSHttpClientFactory = createEbMSHttpClientFactory(clientAlias);
			return ebMSHttpClientFactory.getObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private EbMSHttpClientFactory createEbMSHttpClientFactory(String clientAlias) throws Exception
	{
		EbMSHttpClientFactory result = new EbMSHttpClientFactory();
		result.setType(type.name());
		result.setSslFactoryManager(createSslFactoryManager(clientAlias));
		result.setChunkedStreamingMode(chunkedStreamingMode);
		result.setBase64Writer(base64Writer);
		result.setProxy(proxy);
		result.setEnabledProtocols(enabledProtocols);
		result.setEnabledCipherSuites(enabledCipherSuites);
		result.setVerifyHostnames(verifyHostnames);
		return result;
	}

	private SSLFactoryManager createSslFactoryManager(String clientAlias) throws Exception
	{
		SSLFactoryManager result = new SSLFactoryManager();
		result.setKeyStorePath(keyStorePath);
		result.setKeyStorePassword(keyStorePassword);
		result.setTrustStorePath(trustStorePath);
		result.setTrustStorePassword(trustStorePassword);
		result.setVerifyHostnames(verifyHostnames);
		result.setClientAlias(clientAlias);
		result.afterPropertiesSet();
		return result;
	}

	public void setType(String type)
	{
		try
		{
			this.type = EbMSHttpClientType.valueOf(type);
		}
		catch (IllegalArgumentException e)
		{
			this.type = EbMSHttpClientType.DEFAULT;
		}
	}

	public void setChunkedStreamingMode(boolean chunkedStreamingMode)
	{
		this.chunkedStreamingMode = chunkedStreamingMode;
	}
	
	public void setBase64Writer(boolean base64Writer)
	{
		this.base64Writer = base64Writer;
	}

	public void setProxy(EbMSProxy proxy)
	{
		this.proxy = proxy;
	}

	public void setEnabledProtocols(String[] enabledProtocols)
	{
		this.enabledProtocols = enabledProtocols;
	}

	public void setEnabledCipherSuites(String[] enabledCipherSuites)
	{
		this.enabledCipherSuites = enabledCipherSuites;
	}

	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
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
}
