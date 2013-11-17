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

public class MessageQueue<T>
{
	private class QueueEntry<U>
	{
		public Thread thread;
		public U object;
		
		public QueueEntry(Thread thread)
		{
			this.thread = thread;
		}
	}
	
	private LinkedHashMap<String,QueueEntry<T>> queue;
	private int maxEntries = 128;
	private int timeout = 60000;
	
	public void init()
	{
		queue = 
			new LinkedHashMap<String,QueueEntry<T>>(maxEntries + 1,.75F,true)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String,QueueEntry<T>> eldest)
				{
					return size() > maxEntries;
				}
			}
		;
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

	public T get(String correlationId)
	{
		return get(correlationId,timeout);
	}

	public T get(String correlationId, int timeout)
	{
		try
		{
			Thread.sleep(timeout);
			//Thread.currentThread().wait(timeout);
		}
		catch (InterruptedException e)
		{
		}
		T result = null;
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
				result = queue.remove(correlationId).object;
		}
		return result;
	}

	public void put(String correlationId, T object)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
			{
				queue.get(correlationId).object = object;
				queue.get(correlationId).thread.interrupt();
				//queue.get(correlationId).thread.notify();
			}
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
