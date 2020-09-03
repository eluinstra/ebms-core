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
package nl.clockwork.ebms.delivery.task;

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
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class JMSDeliveryTaskManager implements DeliveryTaskManager
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	@RequiredArgsConstructor
	public class DeliveryTaskMessageCreator implements MessageCreator
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
				result.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delay);
			result.setStringProperty("cpaId",deliveryTask.getCpaId());
			result.setStringProperty("sendDeliveryChannelId",deliveryTask.getSendDeliveryChannelId());
			result.setStringProperty("receiveDeliveryChannelId",deliveryTask.getReceiveDeliveryChannelId());
			result.setStringProperty("messageId",deliveryTask.getMessageId());
			result.setStringProperty("timeToLive",deliveryTask.getTimeToLive() != null ? deliveryTask.getTimeToLive().toString() : null);
			result.setStringProperty("timestamp",deliveryTask.getTimestamp().toString());
			result.setBooleanProperty("confidential",deliveryTask.isConfidential());
			result.setIntProperty("retries",deliveryTask.getRetries());
			return result;
		}
	}

	public static final String JMS_DESTINATION_NAME = "DELIVERY_TASK";
	@NonNull
	JmsTemplate jmsTemplate;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	CPAManager cpaManager;
	int nrAutoRetries;
	long autoRetryInterval;

	@Override
	public void createTask(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		DeliveryTask task = new DeliveryTask(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0);
		jmsTemplate.send(JMS_DESTINATION_NAME,new DeliveryTaskMessageCreator(task));
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status)
	{
		updateTask(task,url,status,null);
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage)
	{
		val deliveryChannel = cpaManager.getDeliveryChannel(
				task.getCpaId(),
				task.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",task.getCpaId(),task.getReceiveDeliveryChannelId()));
		deliveryTaskDAO.insertLog(task.getMessageId(),task.getTimestamp(), url, status, errorMessage);
		if (task.getTimeToLive() != null && CPAUtils.isReliableMessaging(deliveryChannel))
		{
			val nextTask = createNextTask(task,deliveryChannel);
			jmsTemplate.send(JMS_DESTINATION_NAME,new DeliveryTaskMessageCreator(nextTask,calculateDelay(nextTask)));
		}
		else
		{
			switch(ebMSDAO.getMessageAction(task.getMessageId()).orElse(null))
			{
				case ACKNOWLEDGMENT:
				case MESSAGE_ERROR:
					if (task.getRetries() < nrAutoRetries)
					{
						val nextTask = createNextTask(task,autoRetryInterval);
						jmsTemplate.send(JMS_DESTINATION_NAME,new DeliveryTaskMessageCreator(nextTask,autoRetryInterval));
						break;
					}
				default:
			}
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
	}
}
