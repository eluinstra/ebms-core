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
	
	public EbMSMessage getMessage(EbMSMessage message)
	{
		return getMessage(message,1000);
	}

	public EbMSMessage getMessage(EbMSMessage message, int sleepTime)
	{
		EbMSMessage result = null;
		String messageId = message.getMessageHeader().getMessageData().getMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
				throw new RuntimeException("key " + messageId + " already exists!");
			queue.put(messageId,new QueueEntry(Thread.currentThread()));
		}
		try
		{
			Thread.sleep(sleepTime);
		}
		catch (InterruptedException e)
		{
		}
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
			{
				result = queue.get(messageId).message;
				queue.remove(messageId);
			}
		}
		return result;
	}

	public void putMessage(EbMSMessage message)
	{
		String messageId = message.getMessageHeader().getMessageData().getMessageId();
		synchronized (queue)
		{
			if (queue.containsKey(messageId))
			{
				queue.get(messageId).message = message;
				queue.get(messageId).thread.interrupt();
			}
		}
	}
	
	public void setMaxEntries(int maxEntries)
	{
		this.maxEntries = maxEntries;
	}
}
