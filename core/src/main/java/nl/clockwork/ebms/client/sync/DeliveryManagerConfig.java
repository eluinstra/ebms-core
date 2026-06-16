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
package nl.clockwork.ebms.client.sync;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.api.DeliveryManager;
import nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryManagerConfig
{
	public enum DeliveryManagerType
	{
		DEFAULT, JMS, KAFKA;
	}

	@Value("${deliveryManager.minThreads}")
	Integer minThreads;
	@Value("${deliveryManager.maxThreads}")
	Integer maxThreads;
	@Value("${messageQueue.maxEntries}")
	int maxEntries;
	@Value("${messageQueue.timeout}")
	int timeout;

	@Bean("deliveryManagerTaskExecutor")
	public ThreadPoolTaskExecutor deliveryManagerTaskExecutor()
	{
		val result = new ThreadPoolTaskExecutor();
		result.setCorePoolSize(minThreads);
		result.setMaxPoolSize(maxThreads);
		result.setQueueCapacity(maxThreads * 2);
		result.setWaitForTasksToCompleteOnShutdown(true);
		return result;
	}

	@Bean
	@Conditional(DefaultDeliveryManagerType.class)
	public DeliveryManager defaultDeliveryManager(CPAManager cpaManager, EbMSHttpClientFactory ebMSClientFactory)
	{
		return DefaultDeliveryManager.builder()
				.messageQueue(new EbMSMessageQueue(maxEntries, timeout))
				.cpaManager(cpaManager)
				.ebMSClientFactory(ebMSClientFactory)
				.build();
	}

	public static class DefaultDeliveryManagerType implements Condition
	{
		@Override
		public boolean matches(@org.jspecify.annotations.NonNull ConditionContext context, @org.jspecify.annotations.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryManager.type", DeliveryManagerType.class, DeliveryManagerType.DEFAULT)
					== DeliveryManagerType.DEFAULT;
		}
	}
}
