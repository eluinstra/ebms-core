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

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import com.google.common.base.Splitter;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventListenerConfig
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT;
	}
	public enum JMSDestinationType
	{
		QUEUE, TOPIC;
	}
	@Value("${eventListener.type}")
	EventListenerType eventListenerType;
	@Value("${eventListener.filter}")
	String eventListenerFilter;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	ConnectionFactory connectionFactory;
	@Value("${jms.destinationType}")
	JMSDestinationType jmsDestinationType;
	@Autowired
	SQLQueryFactory queryFactory;

	@Bean
	public EventListener eventListener()
	{
		val jmsTemplate = new JmsTemplate(connectionFactory);
		val filter = Splitter.on(',').trimResults().omitEmptyStrings().splitToStream(eventListenerFilter)
			.map(e -> EbMSMessageEventType.valueOf(e))
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(EbMSMessageEventType.class)));
		val eventListener = Match(eventListenerType).of(
				Case($(EventListenerType.DAO),o -> new DAOEventListener(ebMSMessageEventDAO())),
				Case($(EventListenerType.SIMPLE_JMS),o -> new SimpleJMSEventListener(jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType))),
				Case($(EventListenerType.JMS),o -> new JMSEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType))),
				Case($(EventListenerType.JMS_TEXT),o -> new JMSTextEventListener(ebMSDAO,jmsTemplate,createEbMSMessageEventDestinations(jmsDestinationType))),
				Case($(),o -> new LoggingEventListener()));
		return filter.size() > 0 ? new EventListenerFilter(filter,eventListener): eventListener;
	}

	@Bean
	public EbMSMessageEventDAO ebMSMessageEventDAO()
	{
		return new EbMSMessageEventDAOImpl(queryFactory);
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
