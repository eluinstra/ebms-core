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
package nl.clockwork.ebms.delivery.client.apache;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.delivery.client.SSLFactoryManager;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class SSLConnectionSocketFactoryFactory implements FactoryBean<SSLConnectionSocketFactory>
{
	@NonNull
	SSLFactoryManager sslFactoryManager;
	@NonNull
	@Default
	String[] enabledProtocols = new String[]{};
	@NonNull
	@Default
	String[] enabledCipherSuites = new String[]{};
	boolean verifyHostnames;

	@Override
	public SSLConnectionSocketFactory getObject() throws Exception
	{
		return new SSLConnectionSocketFactory(sslFactoryManager.getSslSocketFactory(),enabledProtocols.length == 0 ? null : enabledProtocols,enabledCipherSuites.length == 0 ? null : enabledCipherSuites,getHostnameVerifier());
	}

	private HostnameVerifier getHostnameVerifier()
	{
		return verifyHostnames ? HttpsURLConnection.getDefaultHostnameVerifier() : (h,s) -> true;
	}
	
	@Override
	public Class<?> getObjectType()
	{
		return SSLConnectionSocketFactory.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}
}
