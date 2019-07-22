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
package nl.clockwork.ebms.common;

import java.util.LinkedHashMap;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.Constants;

public class MessageQueue<T> implements InitializingBean
{
	private class QueueEntry<U>
	{
		private Thread thread;
		private U object;
		
		public QueueEntry(Thread thread)
		{
			this.setThread(thread);
		}

		public Thread getThread()
		{
			return thread;
		}

		public void setThread(Thread thread)
		{
			this.thread = thread;
		}

		public U getObject()
		{
			return object;
		}

		public void setObject(U object)
		{
			this.object = object;
		}
	}
	
	private LinkedHashMap<String,QueueEntry<T>> queue;
	private int maxEntries = 128;
	private static final float LOADFACTOR = .75F;
	private int timeout = Constants.MINUTE_IN_MILLIS;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		queue = new LinkedHashMap<String,QueueEntry<T>>(maxEntries + 1, LOADFACTOR, true)
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
				throw new RuntimeException("key " + correlationId + " already exists!");
			queue.put(correlationId,new QueueEntry<T>(Thread.currentThread()));
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
			//Thread.currentThread().wait(timeout);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
		Optional<T> result = Optional.empty();
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
				result = Optional.ofNullable(queue.remove(correlationId).getObject());
		}
		return result;
	}

	public void put(String correlationId, T object)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
			{
				queue.get(correlationId).setObject(object);
				queue.get(correlationId).getThread().interrupt();
				//queue.get(correlationId).thread.notify();
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
	
	public void setMaxEntries(int maxEntries)
	{
		this.maxEntries = maxEntries;
	}
	
	public void setTimeout(int timeout)
	{
		this.timeout = timeout;
	}

}
