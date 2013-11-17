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
package nl.clockwork.ebms.client.apache;

import nl.clockwork.ebms.client.SSLFactoryManager;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.springframework.beans.factory.FactoryBean;

public class SSLSocketFactoryFactory implements FactoryBean<SSLSocketFactory>
{
	private SSLFactoryManager sslFactoryManager;
	private boolean verifyHostnames;

	@Override
	public SSLSocketFactory getObject() throws Exception
	{
		return new SSLSocketFactory(sslFactoryManager.getSslSocketFactory(),verifyHostnames ? SSLSocketFactory.STRICT_HOSTNAME_VERIFIER : SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	}

	@Override
	public Class<?> getObjectType()
	{
		return SSLSocketFactory.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

	public void setSslFactoryManager(SSLFactoryManager sslFactoryManager)
	{
		this.sslFactoryManager = sslFactoryManager;
	}
	
	public void setVerifyHostnames(boolean verifyHostnames)
	{
		this.verifyHostnames = verifyHostnames;
	}
}
