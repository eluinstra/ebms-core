package nl.clockwork.mule.ebms.model;

public class EbMSMessageContext
{
	private String conversationId;
	//private String messageId;
	//private boolean relateMessage;

	public EbMSMessageContext(String conversationId)
	{
		this.conversationId = conversationId;
	}

	public String getConversationId()
	{
		return conversationId;
	}
}
