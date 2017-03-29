package nl.clockwork.ebms.model;

import java.io.Serializable;

import nl.clockwork.ebms.Constants.EbMSMessageEventType;

public class EbMSMessageEvent implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String messageId;
	private EbMSMessageEventType type;
	
	public EbMSMessageEvent(String messageId, EbMSMessageEventType type)
	{
		this.messageId = messageId;
		this.type = type;
	}
	public String getMessageId()
	{
		return messageId;
	}
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	public EbMSMessageEventType getType()
	{
		return type;
	}
	public void setType(EbMSMessageEventType type)
	{
		this.type = type;
	}
}
