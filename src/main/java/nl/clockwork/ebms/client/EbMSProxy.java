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

import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

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
		return StringUtils.isNotBlank(host);
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
