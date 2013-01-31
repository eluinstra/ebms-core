package nl.clockwork.ebms.client;

import nl.clockwork.ebms.model.EbMSMessage;

public interface EbMSClient
{
	public void sendMessage(EbMSMessage message) throws Exception;
}
