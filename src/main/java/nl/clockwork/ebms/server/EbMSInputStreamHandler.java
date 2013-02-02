package nl.clockwork.ebms.server;

import java.io.InputStream;

import nl.clockwork.ebms.processor.EbMSProcessorException;

public interface EbMSInputStreamHandler
{
	void handle(InputStream request) throws EbMSProcessorException;
}
