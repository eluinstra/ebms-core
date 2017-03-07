package nl.clockwork.ebms.client;

import nl.clockwork.ebms.ssl.SSLFactoryManager;

import org.springframework.beans.factory.FactoryBean;

public class EbMSHttpClientFactory implements FactoryBean<EbMSClient>
{
	public enum EbMSHttpClientType
	{
		DEFAULT, APACHE;
	}

	private EbMSHttpClientType type;
	private SSLFactoryManager sslFactoryManager;
	private boolean chunkedStreamingMode;
	private boolean base64Writer;
	private EbMSProxy proxy;
	private String[] enabledProtocols = new String[]{};
	private String[] enabledCipherSuites = new String[]{};
	private boolean verifyHostnames;

	@Override
	public EbMSClient getObject() throws Exception
	{
		if (EbMSHttpClientType.APACHE.equals(type))
			return new nl.clockwork.ebms.client.apache.EbMSHttpClient(sslFactoryManager,enabledProtocols,enabledCipherSuites,verifyHostnames,chunkedStreamingMode,proxy);
		else
			return new EbMSHttpClient(sslFactoryManager,chunkedStreamingMode,base64Writer,proxy);
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSClient.class;
	}

	@Override
	public boolean isSingleton()
	{
		return false;
	}

	public void setType(String type)
	{
		try
		{
			this.type = EbMSHttpClientType.valueOf(type);
		}
		catch (IllegalArgumentException e)
		{
		}
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
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
}
