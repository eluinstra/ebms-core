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
package nl.clockwork.ebms.common.event;

import java.util.Objects;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageEventListenerConfig
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT
	}

	@Value("${eventListener.filter}")
	String eventListenerFilter;

	@Bean
	@Conditional(DefaultEventListenerType.class)
	public MessageEventListener defaultMessageEventListener()
	{
		return new LoggingMessageEventListener();
	}

	@Bean
	@Conditional(DaoEventListenerType.class)
	public MessageEventListener daoMessageEventListener(MessageEventDAO messageEventDAO)
	{
		return new DAOMessageEventListener(messageEventDAO);
	}

	@Bean
	public MessageEventDAO messageEventDAO(DataSource dataSource)
	{
		return new MessageEventDAOImpl(new JdbcTemplate(Objects.requireNonNull(dataSource)));
	}

	@Bean
	public MessageEventListenerFilterProcessor messageEventListenerFilterProcessor()
	{
		return new MessageEventListenerFilterProcessor(eventListenerFilter);
	}

	public static class DefaultEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return "DEFAULT".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}

	public static class DaoEventListenerType implements Condition
	{
		@Override
		public boolean matches(@org.springframework.lang.NonNull ConditionContext context, @org.springframework.lang.NonNull AnnotatedTypeMetadata metadata)
		{
			return "DAO".equalsIgnoreCase(context.getEnvironment().getProperty("eventListener.type", "DEFAULT"));
		}
	}
}
