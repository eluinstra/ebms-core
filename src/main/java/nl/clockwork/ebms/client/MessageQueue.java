package nl.clockwork.ebms.client;

import java.util.LinkedHashMap;

import nl.clockwork.ebms.model.EbMSMessage;

public class MessageQueue
{
	private class QueueEntry
	{
		public Thread thread;
		public EbMSMessage message;
		
		public QueueEntry(Thread thread)
		{
			this.thread = thread;
		}
	}
	
	private LinkedHashMap<String,QueueEntry> queue;
	private int maxEntries = 128;
	private int timeout = 60000;
	
	public void init()
	{
		queue = 
			new LinkedHashMap<String,QueueEntry>(maxEntries + 1,.75F,true)
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<String,QueueEntry> eldest)
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
			queue.put(correlationId,new QueueEntry(Thread.currentThread()));
		}
	}

	public EbMSMessage getMessage(String correlationId)
	{
		return getMessage(correlationId,timeout);
	}

	public EbMSMessage getMessage(String correlationId, int timeout)
	{
		try
		{
			Thread.sleep(timeout);
			//Thread.currentThread().wait(timeout);
		}
		catch (InterruptedException e)
		{
		}
		EbMSMessage result = null;
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
				result = queue.remove(correlationId).message;
		}
		return result;
	}

	public void putMessage(String correlationId, EbMSMessage message)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
			{
				queue.get(correlationId).message = message;
				queue.get(correlationId).thread.interrupt();
				//queue.get(correlationId).thread.notify();
			}
		}
	}
	
	public void putEmptyMessage(String correlationId)
	{
		synchronized (queue)
		{
			if (queue.containsKey(correlationId))
			{
				queue.get(correlationId).message = null;
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
