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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
@AllArgsConstructor
class EbMSProxyFactory implements FactoryBean<EbMSProxy>
{
	String host;
	int port;
	String username;
	String password;
	Set<String> nonProxyHosts;

	@Override
	public EbMSProxy getObject()
	{
		if (StringUtils.isNotBlank(host))
			return EbMSProxy.of(host,port,username,password,nonProxyHosts);
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
