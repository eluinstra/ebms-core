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
import java.util.ArrayList;
import java.util.concurrent.Future;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
class EventTaskExecutor implements Runnable
{
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	EventHandler eventHandler;
	@NonNull TimedTask timedTask;
	int maxEvents;
	String serverId;

	@Builder
	public EventTaskExecutor(@NonNull EbMSEventDAO ebMSEventDAO, @NonNull EventHandler eventHandler, @NonNull TimedTask timedTask, int maxEvents, String serverId)
	{
		this.ebMSEventDAO = ebMSEventDAO;
		this.eventHandler = eventHandler;
		this.timedTask = timedTask;
		this.maxEvents = maxEvents;
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
					val events = maxEvents > 0 ? ebMSEventDAO.getEventsBefore(timestamp,serverId,maxEvents) : ebMSEventDAO.getEventsBefore(timestamp,serverId);
					for (EbMSEvent event : events)
						futures.add(eventHandler.handleAsync(event));
				}
				catch (Exception e)
				{
					log.error("",e);
				}
				futures.forEach(f -> Try.of(() -> f.get()).onFailure(e -> log.error("",e)));
  		};
  		try
  		{
	  		timedTask.run(runnable);
  		}
  		catch(Exception e)
  		{
				log.error("",e);
  		}
		}
	}
}
