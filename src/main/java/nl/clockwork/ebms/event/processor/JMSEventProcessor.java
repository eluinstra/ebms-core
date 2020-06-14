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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class JMSEventProcessor implements Runnable
{
	JmsTemplate jmsTemplate;
	@NonNull
	HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype;
	private String jmsDestinationName;

	@Builder(setterPrefix = "set")
	public JMSEventProcessor(
			boolean start,
			@NonNull EventManagerType type,
			@NonNull JmsTemplate jmsTemplate,
			String jmsDestinationName,
			int maxThreads,
			@NonNull HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype)
	{
		if (start && type == EventManagerType.JMS)
		{
			this.jmsTemplate = jmsTemplate;
			//jmsTemplate.setDefaultDestinationName(jmsDestinationName);
			IntStream.range(0,maxThreads).forEach(i -> startDeamon());
			
		}
		else
			this.jmsTemplate = null;
		this.jmsDestinationName = StringUtils.isEmpty(jmsDestinationName) ? JMSEventManager.JMS_DESTINATION_NAME : jmsDestinationName;
		this.handleEventTaskPrototype = handleEventTaskPrototype;
	}

	private void startDeamon()
	{
		val thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

  public void run()
  {
  	while (true)
  	{
			try
			{
				val message = jmsTemplate.receive(jmsDestinationName);
				if (message != null)
				{
					val event = createEvent(message);
					val task = handleEventTaskPrototype.setEvent(event).build();
					task.run();
					message.acknowledge();
				}
			}
			catch (Exception e)
			{
				log.trace("",e);
			}
  	}
  }

	private EbMSEvent createEvent(Message message) throws JMSException
	{
		val result = EbMSEvent.builder()
				.cpaId(message.getStringProperty("cpaId"))
				.sendDeliveryChannelId(message.getStringProperty("sendDeliveryChannelId"))
				.receiveDeliveryChannelId(message.getStringProperty("receiveDeliveryChannelId"))
				.messageId(message.getStringProperty("messageId"))
				.timeToLive(message.getStringProperty("timeToLive") != null ? Instant.parse(message.getStringProperty("timeToLive")) : null)
				.timestamp(Instant.parse(message.getStringProperty("timestamp")))
				.confidential(message.getBooleanProperty("confidential"))
				.retries(message.getIntProperty("retries"))
				.build();
		return result;
	}
}
