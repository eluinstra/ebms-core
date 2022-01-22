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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class TimedTask
{
	long executionInterval;
	
	public void run(Runnable task)
	{
		if (executionInterval > 0)
		{
			val start = Instant.now();
			task.run();
			val end = Instant.now();
			val sleep = executionInterval - ChronoUnit.MILLIS.between(start,end);
			sleep(sleep);
		}
		else
			task.run();
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
