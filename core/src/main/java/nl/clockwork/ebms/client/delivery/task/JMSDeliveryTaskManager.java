/*
 * Copyright 2011 - 2026 Clockwork
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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import nl.clockwork.ebms.client.EbMSDAO;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPAUtils;
import nl.clockwork.ebms.util.StreamUtils;
import org.apache.activemq.ScheduledMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class JMSDeliveryTaskManager extends DeliveryTaskManager
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@RequiredArgsConstructor
	public static class DeliveryTaskMessageCreator implements MessageCreator
	{
		@NonNull
		DeliveryTask deliveryTask;
		@NonFinal
		Long delay;

		@Override
		public Message createMessage(Session session) throws JMSException
		{
			val result = session.createMessage();
			if (delay != null)
				result.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
			result.setStringProperty("cpaId", deliveryTask.getCpaId());
			result.setStringProperty("sendDeliveryChannelId", deliveryTask.getSendDeliveryChannelId());
			result.setStringProperty("receiveDeliveryChannelId", deliveryTask.getReceiveDeliveryChannelId());
			result.setStringProperty("messageId", deliveryTask.getMessageId());
			result.setStringProperty("timeToLive", deliveryTask.getTimeToLive() != null ? deliveryTask.getTimeToLive().toString() : null);
			result.setStringProperty("timestamp", deliveryTask.getTimestamp().toString());
			result.setBooleanProperty("confidential", deliveryTask.isConfidential());
			result.setIntProperty("retries", deliveryTask.getRetries());
			return result;
		}
	}

	public static final String JMS_DESTINATION_NAME = "DELIVERY_TASK";
	@NonNull
	JmsTemplate jmsTemplate;

	public JMSDeliveryTaskManager(
			@NonNull JmsTemplate jmsTemplate,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull CPAManager cpaManager,
			int nrAutoRetries,
			long autoRetryInterval)
	{
		super(ebMSDAO, deliveryTaskDAO, cpaManager, nrAutoRetries, autoRetryInterval);
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public void insertTask(DeliveryTask task)
	{
		jmsTemplate.send(JMS_DESTINATION_NAME, new DeliveryTaskMessageCreator(task));
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status)
	{
		updateTask(task, url, status, null);
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage)
	{
		updateTask(task, url, status, errorMessage, null);
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage, Exception e)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(task.getCpaId(), task.getReceiveDeliveryChannelId())
				.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel", task.getCpaId(), task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(), task.getTimestamp(), url, status, errorMessage);
		val reliableMessaging = CPAUtils.isReliableMessaging(deliveryChannel);
		if (task.getTimeToLive() != null && reliableMessaging)
		{
			val nextTask = createNextTask(task, deliveryChannel);
			jmsTemplate.send(JMS_DESTINATION_NAME, new DeliveryTaskMessageCreator(nextTask, calculateDelay(nextTask)));
		}
		else if (shouldRetryUnreliable(task, status, e, reliableMessaging))
		{
			val nextTask = createNextTask(task, autoRetryInterval);
			jmsTemplate.send(JMS_DESTINATION_NAME, new DeliveryTaskMessageCreator(nextTask, autoRetryInterval));
		}
	}

	private long calculateDelay(final DeliveryTask task)
	{
		val result = task.getTimestamp().toEpochMilli() - Instant.now().toEpochMilli();
		return result <= 0 ? -1 : result;
	}

	@Override
	public void deleteTask(String messageId)
	{
		// do nothing
	}
}