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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSThreadPoolExecutor;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class EbMSEventProcessor implements Runnable
{
	int period;
	int maxEvents;
	@NonNull
	EbMSEventDAO ebMSEventDAO;
	@NonNull
	HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype;
	ExecutorService executorService;
	String serverId;

	@Builder(setterPrefix = "set")
	public EbMSEventProcessor(
			boolean startEbMSClient,
			@NonNull EventManagerType type,
			int period,
			int maxEvents,
			@NonNull EbMSThreadPoolExecutor ebMSThreadPoolExecutor,
			@NonNull EbMSEventDAO ebMSEventDAO,
			@NonNull HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype,
			String serverId)
	{
		this.period = period;
		this.maxEvents = maxEvents;
		this.ebMSEventDAO = ebMSEventDAO;
		this.handleEventTaskPrototype = handleEventTaskPrototype;
		this.serverId = serverId;
		if (startEbMSClient && type == EventManagerType.DEFAULT)
		{
			this.executorService = ebMSThreadPoolExecutor.createInstance();
			val thread = new Thread(this);
			thread.setDaemon(true);
			thread.start();
		}
		else
			this.executorService = null;
	}

  public void run()
  {
  	while (true)
  	{
  		val start = Instant.now();
			val futures = new ArrayList<Future<?>>();
			try
			{
				val timestamp = Instant.now();
				val events = maxEvents > 0 ? ebMSEventDAO.getEventsBefore(timestamp,serverId,maxEvents) : ebMSEventDAO.getEventsBefore(timestamp,serverId);
				for (EbMSEvent event : events)
					futures.add(executorService.submit(handleEventTaskPrototype.setEvent(event).build()));
			}
			catch (Exception e)
			{
				log.error("",e);
			}
			futures.forEach(f -> Try.of(() -> f.get()).onFailure(e -> log.error("",e)));
			val end = Instant.now();
			val sleep = period - ChronoUnit.MILLIS.between(start,end);
			sleep(sleep);
  	}
  }

	private void sleep(long millis)
	{
		try
		{
			if (millis > 0)
				Thread.sleep(millis);
		}
		catch (InterruptedException e)
		{
			log.trace("",e);
		}
	}
}
