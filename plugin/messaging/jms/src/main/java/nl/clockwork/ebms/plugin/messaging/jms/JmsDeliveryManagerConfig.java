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

import jakarta.jms.ConnectionFactory;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.delivery.DeliveryManager;
import nl.clockwork.ebms.client.delivery.DeliveryManagerConfig.DeliveryManagerType;
import nl.clockwork.ebms.client.delivery.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JmsDeliveryManagerConfig
{
	@Bean
	@Conditional(JmsDeliveryManagerType.class)
	public DeliveryManager jmsDeliveryManager(
			@org.springframework.lang.NonNull ConnectionFactory connectionFactory,
			CPAManager cpaManager,
			EbMSHttpClientFactory ebMSClientFactory,
			@Qualifier("jmsTransactionManager") PlatformTransactionManager transactionManager)
	{
		return JMSDeliveryManager.jmsDeliveryManagerBuilder()
				.cpaManager(cpaManager)
				.ebMSClientFactory(ebMSClientFactory)
				.transactionManager(transactionManager)
				.jmsTemplate(new JmsTemplate(connectionFactory))
				.build();
	}

	public static class JmsDeliveryManagerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryManager.type", DeliveryManagerType.class, DeliveryManagerType.DEFAULT) == DeliveryManagerType.JMS;
		}
	}
}
