package nl.clockwork.mule.ebms.model;

import java.util.Date;

public class EbMSSendEvent
{
	private String messageId;
	private Date time;
	
	public EbMSSendEvent(String messageId, Date time)
	{
		this.messageId = messageId;
		this.time = time;
	}

	public String getMessageId()
	{
		return messageId;
	}
	
	public Date getTime()
	{
		return time;
	}

}
