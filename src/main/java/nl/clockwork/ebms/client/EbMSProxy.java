package nl.clockwork.ebms.client;

import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

public class EbMSProxy
{
	private String host;
	private int port;
	private String username;
	private String password;
	private Set<String> nonProxyHosts;
	
	public EbMSProxy()
	{
	}

	public EbMSProxy(String host, int port, String username, String password, Set<String> nonProxyHosts)
	{
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.nonProxyHosts = nonProxyHosts;
	}

	public String getHost()
	{
		return host;
	}
	public void setHost(String host)
	{
		this.host = host;
	}
	public int getPort()
	{
		return port;
	}
	public void setPort(int port)
	{
		this.port = port;
	}
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	public Set<String> getNonProxyHosts()
	{
		return nonProxyHosts;
	}
	public void setNonProxyHosts(Set<String> nonProxyHosts)
	{
		this.nonProxyHosts = nonProxyHosts;
	}
	public boolean useProxy(String url)
	{
		return StringUtils.isNotBlank(host) && !nonProxyHosts.contains(url);
	}
	public boolean useProxyAuthorization()
	{
		return StringUtils.isNotBlank(username);
	}
	public String getProxyAuthorizationKey()
	{
		return "Proxy-Authorization";
	}
	public String getProxyAuthorizationValue()
	{
		return "Basic " + Base64.encodeBase64String((username + ":" + password).getBytes());
	}
}
