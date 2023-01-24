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
package nl.clockwork.ebms.delivery;


import java.util.LinkedHashMap;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MessageQueue<T>
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@RequiredArgsConstructor
	@Getter
	private class QueueEntry<U>
	{
		private Thread thread;
		@NonFinal
		@Setter
		private U object;
	}

	private static final float LOAD_FACTOR = .75F;
	int timeout;
	@NonNull
	LinkedHashMap<String,QueueEntry<T>> queue;

	public MessageQueue(int maxEntries, int timeout)
	{
		this.timeout = timeout;
		this.queue = new LinkedHashMap<String,QueueEntry<T>>(maxEntries + 1,LOAD_FACTOR,true)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<String,QueueEntry<T>> eldest)
			{
				return size() > maxEntries;
			}
		};
	}

	public void register(String correlationId)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
				throw new IllegalStateException("key " + correlationId + " already exists!");
			queue.put(correlationId,new QueueEntry<>(Thread.currentThread()));
		}
	}

	public Optional<T> get(String correlationId)
	{
		return get(correlationId,timeout);
	}

	public Optional<T> get(String correlationId, int timeout)
	{
		try
		{
			Thread.sleep(timeout);
			// Thread.currentThread().wait(timeout);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
				return Optional.ofNullable(queue.remove(correlationId).getObject());
		}
		return Optional.empty();
	}

	public void put(String correlationId, T object)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
			{
				queue.get(correlationId).setObject(object);
				queue.get(correlationId).getThread().interrupt();
				// queue.get(correlationId).thread.notify();
			}
		}
	}

	public void remove(String correlationId)
	{
		synchronized (queue)
		{
			queue.remove(correlationId);
		}
	}
}
