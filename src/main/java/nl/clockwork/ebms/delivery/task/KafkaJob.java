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


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManagerConfig.QuartzKafkaTaskManagerType;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
@Conditional(QuartzKafkaTaskManagerType.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KafkaJob extends QuartzJobBean
{
	@Autowired
	DeliveryTaskManager deliveryTaskManager;
	@Autowired
	@Qualifier("deliveryTaskKafkaTransactionManager")
	KafkaTransactionManager<?,?> transactionManager;

	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			val properties = context.getJobDetail().getJobDataMap();
			val task = QuartzDeliveryTaskManager.createDeliveryTask(properties);
			deliveryTaskManager.insertTask(task);
		}
		catch (Exception e)
		{
			transactionManager.rollback(status);
			throw new JobExecutionException();
		}
		transactionManager.commit(status);
	}
}
