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

import javax.jms.Session;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JMSConfig
{
	@Value("${jms.broker.start}")
	boolean jmsBrokerStart;
	@Value("${jms.broker.config}")
	String jmsBrokerConfig;
	@Value("${jms.brokerURL}")
	String jmsBrokerUrl;
	@Value("${jms.pool.maxConnections}")
	int maxConnections;

	@Bean("brokerFactory")
	public void brokerFactory() throws Exception
	{
		EbMSBrokerFactoryBean.init(jmsBrokerStart,jmsBrokerConfig);
	}

	@Bean(name="jmsTemplate")
	@DependsOn("brokerFactory")
	public JmsTemplate jmsTemplate()
	{
		val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
		pooledConnectionFactory.setMaxConnections(maxConnections);
		return new JmsTemplate(pooledConnectionFactory);
	}

	@Bean(name="transactedJmsTemplate")
	@DependsOn("brokerFactory")
	public JmsTemplate transactedJmsTemplate()
	{
		val pooledConnectionFactory = new PooledConnectionFactory(jmsBrokerUrl);
		pooledConnectionFactory.setMaxConnections(maxConnections);
		val jmsTemplate = new JmsTemplate(pooledConnectionFactory);
		jmsTemplate.setSessionTransacted(true);
		jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return jmsTemplate;
	}
}
