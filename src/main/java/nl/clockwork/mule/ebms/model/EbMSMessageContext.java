package nl.clockwork.mule.ebms.model;

public class EbMSMessageContext
{
	private String conversationId;
	//private String messageId;
	//private boolean relateMessage;
	//private String channelId;
	//private String refChannelId;

	public EbMSMessageContext(String conversationId)
	{
		this.conversationId = conversationId;
	}

	public String getConversationId()
	{
		return conversationId;
	}
}
