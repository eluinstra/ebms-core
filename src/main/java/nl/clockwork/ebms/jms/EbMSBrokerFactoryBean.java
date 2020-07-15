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
package nl.clockwork.ebms.jms;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static nl.clockwork.ebms.Predicates.startsWith;

import java.io.IOException;

import org.apache.activemq.xbean.BrokerFactoryBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

class EbMSBrokerFactoryBean implements DisposableBean
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

	private static Resource createResource(String path) throws IOException
	{
		return Match(path).of(
				Case($(startsWith("classpath:")),o -> new ClassPathResource(path.substring("classpath:".length()))),
				Case($(startsWith("file:")),o -> new FileSystemResource(path.substring("file:".length()))),
				Case($(),o -> new FileSystemResource(path)));
	}

	@Override
	public void destroy() throws Exception
	{
		if (brokerFactoryBean != null)
			brokerFactoryBean.destroy();
	}
}
