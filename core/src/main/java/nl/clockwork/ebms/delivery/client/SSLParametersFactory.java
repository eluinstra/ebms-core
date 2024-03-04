/*
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
package nl.clockwork.ebms.delivery.client;

import javax.net.ssl.SSLParameters;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.beans.factory.FactoryBean;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SSLParametersFactory implements FactoryBean<SSLParameters>
{
	@NonNull
	String[] enabledProtocols;
	@NonNull
	String[] enabledCipherSuites;

	public SSLParametersFactory(String[] enabledProtocols, String[] enabledCipherSuites)
	{
		this.enabledProtocols = enabledProtocols == null ? new String[]{} : enabledProtocols;
		this.enabledCipherSuites = enabledCipherSuites == null ? new String[]{} : enabledCipherSuites;
	}

	@Override
	public SSLParameters getObject()
	{
		val result = new SSLParameters();
		if (enabledProtocols.length > 0)
			result.setProtocols(enabledProtocols);
		if (enabledProtocols.length > 0)
			result.setCipherSuites(enabledCipherSuites);
		return result;
	}

	@Override
	public Class<?> getObjectType()
	{
		return SSLParameters.class;
	}
}
