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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.FactoryBean;

public class EbMSProxyFactory extends EbMSProxy implements FactoryBean<EbMSProxy>
{
	@Override
	public EbMSProxy getObject() throws Exception
	{
		if (StringUtils.isNotBlank(getHost()))
			return new EbMSProxy(getHost(),getPort(),getUsername(),getPassword(),getNonProxyHosts());
		else
			return null;
	}

	@Override
	public Class<?> getObjectType()
	{
		return EbMSProxy.class;
	}

	@Override
	public boolean isSingleton()
	{
		return true;
	}

}
