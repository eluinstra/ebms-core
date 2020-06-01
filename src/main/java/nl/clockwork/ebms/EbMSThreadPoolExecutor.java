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
package nl.clockwork.ebms;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSThreadPoolExecutor
{
	@NonNull
	Integer maxThreads;

	public EbMSThreadPoolExecutor(@NonNull Integer maxThreads)
	{
		maxThreads = maxThreads--;
		if (maxThreads <= 0)
			maxThreads = Runtime.getRuntime().availableProcessors() - 1;
		log.info(this.getClass().getName() + " using " + maxThreads + " threads");
		this.maxThreads = maxThreads;
	}

	public ThreadPoolExecutor createInstance()
	{
		return new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * 4,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
