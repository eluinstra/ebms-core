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
package nl.clockwork.ebms.plugin.messaging.jms;

import java.util.Objects;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class EbMSBrokerFactoryBean implements DisposableBean
{
	private BrokerFactoryBean brokerFactoryBean;

	public EbMSBrokerFactoryBean(boolean jmsBrokerStart, String jmsBrokerConfig) throws Exception
	{
		if (jmsBrokerStart)
		{
			brokerFactoryBean = new BrokerFactoryBean(createResource(jmsBrokerConfig));
			brokerFactoryBean.setStart(true);
			brokerFactoryBean.afterPropertiesSet();
		}
	}

	private static Resource createResource(String path)
	{
		String resourcePath = Objects.requireNonNull(path);
		if (resourcePath.startsWith("classpath:"))
			return new ClassPathResource(Objects.requireNonNull(resourcePath.substring("classpath:".length())));
		if (resourcePath.startsWith("file:"))
			return new FileSystemResource(Objects.requireNonNull(resourcePath.substring("file:".length())));
		return new FileSystemResource(resourcePath);
	}

	@Override
	public void destroy() throws Exception
	{
		if (brokerFactoryBean != null)
			brokerFactoryBean.destroy();
	}
}
