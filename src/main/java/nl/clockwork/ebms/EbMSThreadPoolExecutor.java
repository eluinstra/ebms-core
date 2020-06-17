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

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSThreadPoolExecutor
{
	@NonNull
	Integer minThreads;
	@NonNull
	Integer maxThreads;

	public EbMSThreadPoolExecutor(@NonNull Integer minThreads, @NonNull Integer maxThreads)
	{
		this.minThreads = minThreads;
		this.maxThreads = maxThreads;
	}

	public ThreadPoolExecutor createInstance()
	{
		return new ThreadPoolExecutor(
				minThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * 4,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}
}
