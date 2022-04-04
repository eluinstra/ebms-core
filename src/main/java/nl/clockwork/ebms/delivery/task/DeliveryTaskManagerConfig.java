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
package nl.clockwork.ebms.delivery.task;

import javax.jms.ConnectionFactory;
import javax.sql.DataSource;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.task.DeliveryTaskHandlerConfig.DeliveryTaskHandlerType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryTaskManagerConfig
{
	@Value("${deliveryTaskHandler.type}")
	DeliveryTaskHandlerType deliveryTaskHandlerType;
	@Autowired
	CPAManager cpaManager;
	@Value("${ebms.serverId:#{null}}")
	String serverId;
	@Autowired
	ConnectionFactory connectionFactory;
	@Autowired(required = false)
	Scheduler scheduler;
	@Value("${deliveryTaskManager.nrAutoRetries}")
	int nrAutoRetries;
	@Value("${deliveryTaskManager.autoRetryInterval}")
	int autoRetryInterval;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	DataSource dataSource;
	@Autowired(required=false)
	@Qualifier("deliveryTaskKafkaTemplate")
	KafkaTemplate<String, DeliveryTask> kafkaTemplate;

	@Bean
	@Conditional(DefaultTaskManagerType.class)
	public DeliveryTaskManager defaultDeliveryTaskManager()
	{
		return createDefaultDeliveryTaskManager();
	}

	@Bean
	@Conditional(JmsTaskManagerType.class)
	public DeliveryTaskManager jmsDeliveryTaskManager()
	{
		return new JMSDeliveryTaskManager(new JmsTemplate(connectionFactory),ebMSDAO,deliveryTaskDAO().getObject(),cpaManager,nrAutoRetries,autoRetryInterval);
	}

	@Bean
	@Conditional(QuartzTaskManagerType.class)
	public DeliveryTaskManager quartzDeliveryTaskManager()
	{
		return new QuartzDeliveryTaskManager(scheduler,ebMSDAO,deliveryTaskDAO().getObject(),cpaManager,nrAutoRetries,autoRetryInterval);
	}

	@Bean
	@Conditional(QuartzJMSTaskManagerType.class)
	public DeliveryTaskManager quartzJMSDeliveryTaskManager()
	{
		return new QuartzJMSDeliveryTaskManager(scheduler,ebMSDAO,deliveryTaskDAO().getObject(),cpaManager,nrAutoRetries,autoRetryInterval,new JmsTemplate(connectionFactory));
	}

	@Bean
	@Conditional(QuartzKafkaTaskManagerType.class)
	public DeliveryTaskManager quartzKafkaDeliveryTaskManager()
	{
		return new QuartzKafkaDeliveryTaskManager(scheduler, ebMSDAO, deliveryTaskDAO().getObject(), cpaManager, nrAutoRetries, autoRetryInterval, kafkaTemplate);
	}

	@Bean
	public DeliveryTaskDAOFactory deliveryTaskDAO()
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new DeliveryTaskDAOFactory(dataSource,jdbcTemplate);
	}

	private DAODeliveryTaskManager createDefaultDeliveryTaskManager()
	{
		return new DAODeliveryTaskManager(ebMSDAO,deliveryTaskDAO().getObject(),cpaManager,serverId,nrAutoRetries,autoRetryInterval);
	}

	public static class DefaultTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.DEFAULT;
		}
	}
	public static class JmsTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.JMS;
		}
	}
	public static class QuartzTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ;
		}
	}
	public static class QuartzJMSTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_JMS;
		}
	}
	public static class QuartzKafkaTaskManagerType implements Condition
	{
		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata)
		{
			return context.getEnvironment().getProperty("deliveryTaskHandler.type",DeliveryTaskHandlerType.class,DeliveryTaskHandlerType.DEFAULT) == DeliveryTaskHandlerType.QUARTZ_KAFKA;
		}
	}
}
