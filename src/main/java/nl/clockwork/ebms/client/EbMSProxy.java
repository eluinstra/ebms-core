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

import lombok.NonNull;
import lombok.Value;

@Value(staticConstructor = "of")
public class EbMSProxy
{
	@NonNull
	String host;
	int port;
	String username;
	String password;
	Set<String> nonProxyHosts;

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
