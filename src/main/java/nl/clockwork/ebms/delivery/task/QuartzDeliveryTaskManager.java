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

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
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

	@Override
	public void createTask(String cpaId, DeliveryChannel sendDeliveryChannel, DeliveryChannel receiveDeliveryChannel, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential)
	{
		try
		{
			val job = createJob(cpaId,sendDeliveryChannel.getChannelId(),receiveDeliveryChannel.getChannelId(),messageId,timeToLive,timestamp,isConfidential,0);
			val trigger = newTrigger().withIdentity(TriggerKey.triggerKey(messageId)).startNow().build();
			scheduler.scheduleJob(job,trigger);
		}
		catch (SchedulerException e)
		{
			new IllegalStateException(e);
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
//			JobDetail jobDetail = scheduler.getJobDetail(JobKey.jobKey(task.getMessageId()));
//			jobDetail.getJobDataMap().putAsString("retries",jobDetail.getJobDataMap().getIntegerFromString("retries") + 1);
				val nextTask = createNextTask(task,deliveryChannel);
				val trigger = newTrigger()
						.withIdentity(TriggerKey.triggerKey(nextTask.getMessageId()))
						.startAt(Date.from(nextTask.getTimestamp()))
						.build();
				scheduler.scheduleJob(createJob(
						nextTask.getCpaId(),
						nextTask.getSendDeliveryChannelId(),
						nextTask.getReceiveDeliveryChannelId(),
						nextTask.getMessageId(),
						nextTask.getTimeToLive(),
						nextTask.getTimestamp(),
						nextTask.isConfidential(),
						nextTask.getRetries()),Collections.singleton(trigger),true);
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
							scheduler.scheduleJob(createJob(
									nextTask.getCpaId(),
									nextTask.getSendDeliveryChannelId(),
									nextTask.getReceiveDeliveryChannelId(),
									nextTask.getMessageId(),
									nextTask.getTimeToLive(),
									nextTask.getTimestamp(),
									nextTask.isConfidential(),
									nextTask.getRetries()),Collections.singleton(trigger),true);
							break;
						}
					default:
				}
			}
		}
		catch (SchedulerException e)
		{
			new IllegalStateException(e);
		}
	}

	@Override
	public void deleteTask(String messageId)
	{
		try
		{
			scheduler.deleteJob(JobKey.jobKey(messageId));
		}
		catch (SchedulerException e)
		{
			new IllegalStateException(e);
		}
	}

	private JobDetail createJob(String cpaId, String sendDeliveryChannelId, String receiveDeliveryChannelId, String messageId, Instant timeToLive, Instant timestamp, boolean isConfidential, int retries)
	{
		return newJob(DeliveryTaskJob.class)
				//.storeDurably()
				.withIdentity(JobKey.jobKey(messageId/*,serverId*/))
				.usingJobData("cpaId",cpaId)
				.usingJobData("sendDeliveryChannel",sendDeliveryChannelId)
				.usingJobData("receiveDeliveryChannel",receiveDeliveryChannelId)
				.usingJobData("messageId",messageId)
				.usingJobData("timeToLive",timeToLive != null ? timeToLive.toEpochMilli() : null)
				.usingJobData("timestamp",timestamp.toEpochMilli())
				.usingJobData("isConfidential",isConfidential)
				.usingJobData("retries",retries)
				.build();
	}
}
