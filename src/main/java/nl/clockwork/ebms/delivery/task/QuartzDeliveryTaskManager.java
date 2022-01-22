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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class QuartzDeliveryTaskManager implements DeliveryTaskManager
{
	@NonNull
	Scheduler scheduler;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	CPAManager cpaManager;
	int nrAutoRetries;
	int autoRetryInterval;

	public static DeliveryTask createDeliveryTask(final JobDataMap properties)
	{
		return new DeliveryTask(properties.getString("cpaId"),
				properties.getString("sendDeliveryChannel"),
				properties.getString("receiveDeliveryChannel"),
				properties.getString("messageId"),
				properties.get("timeToLive") != null ? Instant.ofEpochMilli(properties.getLong("timeToLive")) : null,
				Instant.ofEpochMilli(properties.getLong("timestamp")),
				properties.getBoolean("isConfidential"),
				properties.getInt("retries"));
	}

	@Override
	public void insertTask(DeliveryTask task)
	{
		try
		{
			val job = createJob(task);
			val trigger = newTrigger().withIdentity(TriggerKey.triggerKey(task.getMessageId())).startNow().build();
			scheduler.scheduleJob(job,trigger);
		}
		catch (SchedulerException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status)
	{
		updateTask(task,url,status,null);
	}

	@Override
	public void updateTask(DeliveryTask task, String url, DeliveryTaskStatus status, String errorMessage)
	{
		try
		{
			val deliveryChannel = cpaManager.getDeliveryChannel(
					task.getCpaId(),
					task.getReceiveDeliveryChannelId())
						.orElseThrow(() -> StreamUtils.illegalStateException("DeliveryChannel",task.getCpaId(),task.getReceiveDeliveryChannelId()));
			deliveryTaskDAO.insertLog(task.getMessageId(),task.getTimestamp(),url,status,errorMessage);
			val reliableMessaging = CPAUtils.isReliableMessaging(deliveryChannel);
			if (task.getTimeToLive() != null && reliableMessaging)
			{
				val nextTask = createNextTask(task,deliveryChannel);
				val trigger = newTrigger()
						.withIdentity(TriggerKey.triggerKey(nextTask.getMessageId()))
						.startAt(Date.from(nextTask.getTimestamp()))
						.build();
				scheduler.scheduleJob(createJob(nextTask),Collections.singleton(trigger),true);
			}
			else
			{
				switch(ebMSDAO.getMessageAction(task.getMessageId()).orElse(null))
				{
					case ACKNOWLEDGMENT:
					case MESSAGE_ERROR:
						if (!reliableMessaging && task.getRetries() < nrAutoRetries)
						{
							val nextTask = createNextTask(task,autoRetryInterval);
							val trigger = newTrigger()
									.startAt(Date.from(nextTask.getTimestamp()))
									.build();
							scheduler.scheduleJob(createJob(nextTask),Collections.singleton(trigger),true);
							break;
						}
					default:
				}
			}
		}
		catch (SchedulerException e)
		{
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void deleteTask(String messageId)
	{
	}

	protected Class<? extends Job> getJobClass()
	{
		return DeliveryTaskJob.class;
	}

	private JobDetail createJob(DeliveryTask task)
	{
		return newJob(getJobClass())
				.withIdentity(JobKey.jobKey(task.getMessageId()))
				.usingJobData("cpaId",task.getCpaId())
				.usingJobData("sendDeliveryChannel",task.getSendDeliveryChannelId())
				.usingJobData("receiveDeliveryChannel",task.getReceiveDeliveryChannelId())
				.usingJobData("messageId",task.getMessageId())
				.usingJobData("timeToLive",task.getTimeToLive() != null ? task.getTimeToLive().toEpochMilli() : null)
				.usingJobData("timestamp",task.getTimestamp().toEpochMilli())
				.usingJobData("isConfidential",task.isConfidential())
				.usingJobData("retries",task.getRetries())
				.build();
	}
}
