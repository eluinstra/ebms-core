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
	
	public void register(EbMSMessage message)
	{
		String messageId = message.getMessageHeader().getMessageData().getMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
				throw new RuntimeException("key " + messageId + " already exists!");
			queue.put(messageId,new QueueEntry(Thread.currentThread()));
		}
	}

	public EbMSMessage getMessage(EbMSMessage message)
	{
		return getMessage(message,timeout);
	}

	public EbMSMessage getMessage(EbMSMessage message, int timeout)
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
		String messageId = message.getMessageHeader().getMessageData().getMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
				result = queue.remove(messageId).message;
		}
		return result;
	}

	public void putMessage(EbMSMessage message)
	{
		String messageId = message.getMessageHeader().getMessageData().getRefToMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
			{
				queue.get(messageId).message = message;
				queue.get(messageId).thread.interrupt();
				//queue.get(messageId).thread.notify();
			}
		}
	}
	
	public void putEmptyMessage(EbMSMessage message)
	{
		String messageId = message.getMessageHeader().getMessageData().getMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
			{
				queue.get(messageId).message = null;
				queue.get(messageId).thread.interrupt();
				//queue.get(messageId).thread.notify();
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
