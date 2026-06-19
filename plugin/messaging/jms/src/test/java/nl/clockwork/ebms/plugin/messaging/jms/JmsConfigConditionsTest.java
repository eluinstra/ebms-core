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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import nl.clockwork.ebms.client.delivery.DeliveryManagerConfig.DeliveryManagerType;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

class JmsConfigConditionsTest
{
	private static ConditionContext context(Map<String, Object> props)
	{
		final Environment env = mock(Environment.class);
		lenient().when(env.getProperty("eventListener.type", "DEFAULT")).thenReturn((String)props.getOrDefault("eventListener.type", "DEFAULT"));
		lenient().when(env.getProperty("deliveryTaskHandler.type", "DEFAULT")).thenReturn((String)props.getOrDefault("deliveryTaskHandler.type", "DEFAULT"));
		lenient().when(env.getProperty("deliveryTaskHandler.start", Boolean.class, true))
				.thenReturn((Boolean)props.getOrDefault("deliveryTaskHandler.start", Boolean.TRUE));
		lenient().when(env.getProperty("deliveryManager.type", DeliveryManagerType.class, DeliveryManagerType.DEFAULT))
				.thenReturn((DeliveryManagerType)props.getOrDefault("deliveryManager.type", DeliveryManagerType.DEFAULT));
		final ConditionContext ctx = mock(ConditionContext.class);
		when(ctx.getEnvironment()).thenReturn(env);
		return ctx;
	}

	private static AnnotatedTypeMetadata metadata()
	{
		return mock(AnnotatedTypeMetadata.class);
	}

	@Test
	void deliveryManagerConditionMatchesWhenTypeIsJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("deliveryManager.type", DeliveryManagerType.JMS);
		assertThat(new JmsDeliveryManagerConfig.JmsDeliveryManagerType().matches(context(props), metadata())).isTrue();
	}

	@Test
	void deliveryManagerConditionRejectsDefault()
	{
		assertThat(new JmsDeliveryManagerConfig.JmsDeliveryManagerType().matches(context(Map.of()), metadata())).isFalse();
	}

	@Test
	void deliveryManagerConditionRejectsKafka()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("deliveryManager.type", DeliveryManagerType.KAFKA);
		assertThat(new JmsDeliveryManagerConfig.JmsDeliveryManagerType().matches(context(props), metadata())).isFalse();
	}

	@Test
	void taskHandlerConditionMatchesWhenStartTrueAndTypeJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("deliveryTaskHandler.start", Boolean.TRUE);
		props.put("deliveryTaskHandler.type", "JMS");
		assertThat(new JmsDeliveryTaskHandlerConfig.JmsTaskHandlerType().matches(context(props), metadata())).isTrue();
	}

	@Test
	void taskHandlerConditionRejectsWhenStartFalse()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("deliveryTaskHandler.start", Boolean.FALSE);
		props.put("deliveryTaskHandler.type", "JMS");
		assertThat(new JmsDeliveryTaskHandlerConfig.JmsTaskHandlerType().matches(context(props), metadata())).isFalse();
	}

	@Test
	void taskHandlerConditionRejectsWhenTypeIsNotJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("deliveryTaskHandler.start", Boolean.TRUE);
		props.put("deliveryTaskHandler.type", "DEFAULT");
		assertThat(new JmsDeliveryTaskHandlerConfig.JmsTaskHandlerType().matches(context(props), metadata())).isFalse();
	}

	@Test
	void simpleJmsEventListenerConditionMatchesSimpleJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("eventListener.type", "SIMPLE_JMS");
		assertThat(new JmsMessageEventListenerConfig.SimpleJmsEventListenerType().matches(context(props), metadata())).isTrue();
	}

	@Test
	void simpleJmsEventListenerConditionRejectsJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("eventListener.type", "JMS");
		assertThat(new JmsMessageEventListenerConfig.SimpleJmsEventListenerType().matches(context(props), metadata())).isFalse();
	}

	@Test
	void jmsEventListenerConditionMatchesJms()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("eventListener.type", "JMS");
		assertThat(new JmsMessageEventListenerConfig.JmsEventListenerType().matches(context(props), metadata())).isTrue();
	}

	@Test
	void jmsEventListenerConditionRejectsJmsText()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("eventListener.type", "JMS_TEXT");
		assertThat(new JmsMessageEventListenerConfig.JmsEventListenerType().matches(context(props), metadata())).isFalse();
	}

	@Test
	void jmsTextEventListenerConditionMatchesJmsText()
	{
		final Map<String, Object> props = new HashMap<>();
		props.put("eventListener.type", "JMS_TEXT");
		assertThat(new JmsMessageEventListenerConfig.JmsTextEventListenerType().matches(context(props), metadata())).isTrue();
	}

	@Test
	void jmsTextEventListenerConditionRejectsDefault()
	{
		assertThat(new JmsMessageEventListenerConfig.JmsTextEventListenerType().matches(context(Map.of()), metadata())).isFalse();
	}
}
