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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.quartz.Scheduler;
import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.task.JMSDeliveryTaskManager.DeliveryTaskMessageCreator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuartzJMSDeliveryTaskManager extends QuartzDeliveryTaskManager
{
	@NonNull
	JmsTemplate jmsTemplate;

	public QuartzJMSDeliveryTaskManager(@NonNull Scheduler scheduler, @NonNull EbMSDAO ebMSDAO, @NonNull DeliveryTaskDAO deliveryTaskDAO, @NonNull CPAManager cpaManager, int nrAutoRetries, int autoRetryInterval, @NonNull JmsTemplate jmsTemplate)
	{
		super(scheduler,ebMSDAO,deliveryTaskDAO,cpaManager,nrAutoRetries,autoRetryInterval);
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public void createTask(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		DeliveryTask task = new DeliveryTask(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(), messageId, timeToLive, timestamp, isConfidential, 0);
		jmsTemplate.send(JMSDeliveryTaskManager.JMS_DESTINATION_NAME,new DeliveryTaskMessageCreator(task));
	}
}
