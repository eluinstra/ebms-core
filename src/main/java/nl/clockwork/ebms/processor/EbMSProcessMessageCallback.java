package nl.clockwork.ebms.processor;

import nl.clockwork.ebms.model.EbMSMessageContent;

public interface EbMSProcessMessageCallback
{
	void process(EbMSMessageContent messageContent);
}
