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

import io.vavr.control.Try;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class DAODeliveryTaskExecutor implements Runnable
{
	@NonNull
	DeliveryTaskDAO deliveryTaskDAO;
	@NonNull
	DeliveryTaskHandler deliveryTaskHandler;
	@NonNull
	TimedTask timedTask;
	int maxTasks;
	String serverId;

	@Builder
	public DAODeliveryTaskExecutor(
			@NonNull DeliveryTaskDAO deliveryTaskDAO,
			@NonNull DeliveryTaskHandler deliveryTaskHandler,
			@NonNull TimedTask timedTask,
			int maxTasks,
			String serverId)
	{
		this.deliveryTaskDAO = deliveryTaskDAO;
		this.deliveryTaskHandler = deliveryTaskHandler;
		this.timedTask = timedTask;
		this.maxTasks = maxTasks;
		this.serverId = serverId;
		val executor = new ThreadPoolTaskExecutor();
		executor.setDaemon(true);
		executor.setMaxPoolSize(1);
		executor.afterPropertiesSet();
		executor.execute(this);
	}

	public void run()
	{
		while (true)
		{
			Runnable runnable = () ->
			{
				val futures = new ArrayList<Future<?>>();
				try
				{
					val timestamp = Instant.now();
					val tasks = maxTasks > 0 ? deliveryTaskDAO.getTasksBefore(timestamp, serverId, maxTasks) : deliveryTaskDAO.getTasksBefore(timestamp, serverId);
					for (DeliveryTask task : tasks)
						futures.add(deliveryTaskHandler.handleAsync(task));
				}
				catch (Exception e)
				{
					log.error("", e);
				}
				futures.forEach(f -> Try.of(() -> f.get()).onFailure(e -> log.error("", e)));
			};
			try
			{
				timedTask.run(runnable);
			}
			catch (Exception e)
			{
				log.error("", e);
			}
		}
	}
}
