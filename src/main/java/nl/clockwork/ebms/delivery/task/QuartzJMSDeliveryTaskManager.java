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

import org.quartz.Job;
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
	public void insertTask(DeliveryTask task)
	{
		jmsTemplate.send(JMSDeliveryTaskManager.JMS_DESTINATION_NAME,new DeliveryTaskMessageCreator(task));
	}

	@Override
	protected Class<? extends Job> getJobClass()
	{
		return JMSJob.class;
	}
}
