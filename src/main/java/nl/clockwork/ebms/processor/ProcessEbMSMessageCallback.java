package nl.clockwork.ebms.processor;

import nl.clockwork.ebms.model.EbMSMessageContent;

public interface ProcessEbMSMessageCallback
{
	void process(EbMSMessageContent messageContent);
}
