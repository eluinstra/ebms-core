package nl.clockwork.mule.ebms.model;

import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

public class EbMSMessageContext
{
	private String conversationId;
	private String messageId;
	//private boolean relateMessage;
	//private String channelId;
	//private String refChannelId;

	public EbMSMessageContext(MessageHeader messageHeader)
	{
		this(messageHeader.getConversationId(),messageHeader.getMessageData().getMessageId());		
	}
	
	public EbMSMessageContext(String conversationId, String messageId)
	{
		this.conversationId = conversationId;
		this.messageId = messageId;
	}

	public String getConversationId()
	{
		return conversationId;
	}
	
	public String getMessageId()
	{
		return messageId;
	}
}
