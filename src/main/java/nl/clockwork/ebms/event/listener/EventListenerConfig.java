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
package nl.clockwork.ebms.event.listener;

import java.util.Map;
import java.util.stream.Collectors;

import javax.jms.Destination;
import javax.sql.DataSource;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.jms.JMSDestinationType;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventListenerConfig
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT;
	}
	@Value("${eventListener.type}")
	EventListenerType eventListenerType;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	JmsTemplate jmsTemplate;
	@Value("${jms.destinationType}")
	JMSDestinationType jmsDestinationType;
	@Autowired
	DataSource dataSource;
	@Autowired
	SQLQueryFactory queryFactory;

	@Bean
	public EventListener eventListener() throws Exception
	{
		switch (eventListenerType)
		{
			case DAO:
				return new DAOEventListener(ebMSMessageEventDAO());
			case SIMPLE_JMS:
				return new SimpleJMSEventListener(jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
			case JMS:
				return new JMSEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
			case JMS_TEXT:
				return new JMSTextEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType));
			default:
				return new LoggingEventListener();
		}
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO() throws Exception
	{
		val jdbcTemplate = new JdbcTemplate(dataSource);
		return new EbMSMessageEventDAOImpl(jdbcTemplate,queryFactory);
	}

	private Map<String,Destination> createEbMSMessageEventDestinations(JMSDestinationType jmsDestinationType)
	{
		return EbMSMessageEventType.stream()
				.collect(Collectors.toMap(e -> e.name(),e -> createDestination(jmsDestinationType,e)));
	}

	private Destination createDestination(JMSDestinationType jmsDestinationType, EbMSMessageEventType e)
	{
		return jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name());
	}
}
