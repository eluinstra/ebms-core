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
package nl.clockwork.ebms.event;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;

import com.google.common.base.Splitter;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageEventListenerConfig
{
	public enum EventListenerType
	{
		DEFAULT, DAO, SIMPLE_JMS, JMS, JMS_TEXT, KAFKA
	}

	public enum JMSDestinationType
	{
		QUEUE, TOPIC;
	}

	@Value("${eventListener.type}")
	EventListenerType eventListenerType;
	@Value("${eventListener.filter}")
	String eventListenerFilter;
	@Value("${eventListener.jms.destinationType}")
	JMSDestinationType jmsDestinationType;

	@Bean
	public MessageEventListener messageEventListener(
			ConnectionFactory connectionFactory,
			MessageEventDAO messageEventDAO,
			EbMSDAO ebMSDAO,
			@Autowired(required = false) @Qualifier("messagePropertiesKafkaTemplate") KafkaTemplate<String,EbMSMessageProperties> kafkaTemplate)
	{
		val jmsTemplate = new JmsTemplate(connectionFactory);
		val filter = Splitter.on(',')
				.trimResults()
				.omitEmptyStrings()
				.splitToStream(eventListenerFilter)
				.map(MessageEventType::valueOf)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(MessageEventType.class)));
		val eventListener = Match(eventListenerType).of(Case($(EventListenerType.DAO),o -> new DAOMessageEventListener(messageEventDAO)),
				Case($(EventListenerType.SIMPLE_JMS),o -> new SimpleJMSMessageEventListener(jmsTemplate,createMessageEventDestinations(jmsDestinationType))),
				Case($(EventListenerType.JMS),o -> new JMSMessageEventListener(ebMSDAO,jmsTemplate,createMessageEventDestinations(jmsDestinationType))),
				Case($(EventListenerType.JMS_TEXT),o -> new JMSTextMessageEventListener(ebMSDAO,jmsTemplate,createMessageEventDestinations(jmsDestinationType))),
				Case($(EventListenerType.KAFKA),o -> new KafkaMessageEventListener(ebMSDAO,kafkaTemplate)),
				Case($(),o -> new LoggingMessageEventListener()));
		return filter.size() > 0 ? new MessageEventListenerFilter(filter,eventListener) : eventListener;
	}

	@Bean
	public MessageEventDAO messageEventDAO(DataSource dataSource)
	{
		return new MessageEventDAOImpl(new JdbcTemplate(dataSource));
	}

	private Map<String,Destination> createMessageEventDestinations(JMSDestinationType jmsDestinationType)
	{
		return MessageEventType.stream().collect(Collectors.toMap(Enum::name,e -> createDestination(jmsDestinationType,e)));
	}

	private Destination createDestination(JMSDestinationType jmsDestinationType, MessageEventType e)
	{
		return jmsDestinationType == JMSDestinationType.QUEUE ? new ActiveMQQueue(e.name()) : new ActiveMQTopic("VirtualTopic." + e.name());
	}
}
