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
package nl.clockwork.ebms.client.delivery.task;

import jakarta.jms.ConnectionFactory;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.client.delivery.EbMSDAO;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskHandlerConfig.DeliveryTaskHandlerType;
import nl.clockwork.ebms.common.cpa.CPAManager;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskManagerConfig
{
	private static final String DELIVERY_TASK_HANDLER_TYPE = "deliveryTaskHandler.type";

	@Value("${deliveryTaskHandler.type}")
	DeliveryTaskHandlerType deliveryTaskHandlerType;
	@Value("${ebms.serverId:#{null}}")
	String serverId;
	@Value("${deliveryTaskManager.nrAutoRetries}")
	int nrAutoRetries;
	@Value("${deliveryTaskManager.autoRetryInterval}")
	int autoRetryInterval;

	@Bean
	@Conditional(DefaultTaskManagerType.class)
	public DeliveryTaskManager defaultDeliveryTaskManager(DeliveryTaskDAO deliveryTaskDAO, EbMSDAO ebMSDAO, CPAManager cpaManager)
	{
		return createDefaultDeliveryTaskManager(deliveryTaskDAO, ebMSDAO, cpaManager);
	}

	@Bean
	@Conditional(JmsTaskManagerType.class)
	public DeliveryTaskManager jmsDeliveryTaskManager(
			DeliveryTaskDAO deliveryTaskDAO,
			EbMSDAO ebMSDAO,
			CPAManager cpaManager,
			@org.springframework.lang.NonNull ConnectionFactory connectionFactory)
	{
		return new JMSDeliveryTaskManager(new JmsTemplate(connectionFactory), ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
	}

	@Bean
	@Conditional(QuartzTaskManagerType.class)
	public DeliveryTaskManager quartzDeliveryTaskManager(DeliveryTaskDAO deliveryTaskDAO, EbMSDAO ebMSDAO, CPAManager cpaManager, Scheduler scheduler)
	{
		return new QuartzDeliveryTaskManager(scheduler, ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
	}

	@Bean
	@Conditional(QuartzJMSTaskManagerType.class)
	public DeliveryTaskManager quartzJMSDeliveryTaskManager(
			DeliveryTaskDAO deliveryTaskDAO,
			EbMSDAO ebMSDAO,
			CPAManager cpaManager,
			Scheduler scheduler,
			@org.springframework.lang.NonNull ConnectionFactory connectionFactory)
	{
		return new QuartzJMSDeliveryTaskManager(
				scheduler,
				ebMSDAO,
				deliveryTaskDAO,
				cpaManager,
				nrAutoRetries,
				autoRetryInterval,
				new JmsTemplate(connectionFactory));
	}

	@Bean
	public DeliveryTaskDAO deliveryTaskDAO(@org.springframework.lang.NonNull DataSource dataSource)
	{
		return new DeliveryTaskDAOImpl(new JdbcTemplate(dataSource));
	}

	private DAODeliveryTaskManager createDefaultDeliveryTaskManager(DeliveryTaskDAO deliveryTaskDAO, EbMSDAO ebMSDAO, CPAManager cpaManager)
	{
		return new DAODeliveryTaskManager(ebMSDAO, deliveryTaskDAO, cpaManager, serverId, nrAutoRetries, autoRetryInterval);
	}

	public static class DefaultTaskManagerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, DeliveryTaskHandlerType.class, DeliveryTaskHandlerType.DEFAULT)
					== DeliveryTaskHandlerType.DEFAULT;
		}
	}

	public static class JmsTaskManagerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, DeliveryTaskHandlerType.class, DeliveryTaskHandlerType.DEFAULT)
					== DeliveryTaskHandlerType.JMS;
		}
	}

	public static class QuartzTaskManagerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, DeliveryTaskHandlerType.class, DeliveryTaskHandlerType.DEFAULT)
					== DeliveryTaskHandlerType.QUARTZ;
		}
	}

	public static class QuartzJMSTaskManagerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty(DELIVERY_TASK_HANDLER_TYPE, DeliveryTaskHandlerType.class, DeliveryTaskHandlerType.DEFAULT)
					== DeliveryTaskHandlerType.QUARTZ_JMS;
		}
	}
}
