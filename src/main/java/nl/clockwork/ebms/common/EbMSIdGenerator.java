package nl.clockwork.ebms.common;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class EbMSIdGenerator
{
	private final String serverId;
	
	public EbMSIdGenerator(String serverId)
	{
		this.serverId = StringUtils.isBlank(serverId) ? "" : "_" + serverId;
	}

	public String generateMessageId(String hostname)
	{
		return UUID.randomUUID().toString() + serverId + "@" + hostname;
	}

	public String generateMessageId(String hostname, String conversationId)
	{
		return conversationId + "@" + hostname;
	}

	public String generateConversationId()
	{
		return UUID.randomUUID().toString() + serverId;
	}

}
