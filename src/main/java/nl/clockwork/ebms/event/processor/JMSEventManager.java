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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.activemq.ScheduledMessage;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class JMSEventManager implements EventManager
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@RequiredArgsConstructor
	public class EventMessageCreator implements MessageCreator
	{
		@NonNull
		EbMSEvent event;
		@NonFinal
		Long delay;

		@Override
		public Message createMessage(Session session) throws JMSException
		{
			val result = session.createMessage();
			if (delay != null)
				result.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delay);
			result.setStringProperty("cpaId",event.getCpaId());
			result.setStringProperty("sendDeliveryChannelId",event.getSendDeliveryChannelId());
			result.setStringProperty("receiveDeliveryChannelId",event.getReceiveDeliveryChannelId());
			result.setStringProperty("messageId",event.getMessageId());
			result.setStringProperty("timeToLive",event.getTimeToLive() != null ? event.getTimeToLive().toString() : null);
			result.setStringProperty("timestamp",event.getTimestamp().toString());
			result.setBooleanProperty("confidential",event.isConfidential());
			result.setIntProperty("retries",event.getRetries());
			return result;
		}
	}

	public static final String JMS_DESTINATION_NAME = "EVENT";
	@NonNull
	JmsTemplate jmsTemplate;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	CPAManager cpaManager;
	int nrAutoRetries;
	long autoRetryInterval;

	@Override
	public void createEvent(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		EbMSEvent event = new EbMSEvent(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0);
		jmsTemplate.send(JMS_DESTINATION_NAME,new EventMessageCreator(event));
	}

	@Override
	public void updateEvent(EbMSEvent event, String url, EbMSEventStatus status)
	{
		updateEvent(event,url,status,null);
	}

	@Override
	public void updateEvent(EbMSEvent event, String url, EbMSEventStatus status, String errorMessage)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		Action action = () ->
		{
			ebMSEventDAO.insertEventLog(event.getMessageId(),event.getTimestamp(), url, status, errorMessage);
			if (event.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
			{
				val nextEvent = createNextEvent(event,deliveryChannel);
				//jmsTemplate.setDeliveryDelay(calculateDelay(nextEvent));
				jmsTemplate.send(JMS_DESTINATION_NAME,new EventMessageCreator(nextEvent,calculateDelay(nextEvent)));
			}
			else
			{
				switch(ebMSDAO.getMessageAction(event.getMessageId()).orElse(null))
				{
					case ACKNOWLEDGMENT:
					case MESSAGE_ERROR:
						if (event.getRetries() < nrAutoRetries)
						{
							val nextEvent = createNextEvent(event,autoRetryInterval);
							//jmsTemplate.setDeliveryDelay(autoRetryInterval);
							jmsTemplate.send(JMS_DESTINATION_NAME,new EventMessageCreator(nextEvent,autoRetryInterval));
							break;
						}
					default:
				}
			}
		};
		ebMSEventDAO.executeTransaction(action);
	}

	private long calculateDelay(final EbMSEvent event)
	{
		val result = event.getTimestamp().toEpochMilli() - Instant.now().toEpochMilli();
		return result <= 0 ? -1 : result;
	}

	@Override
	public void deleteEvent(String messageId)
	{
	}
}
