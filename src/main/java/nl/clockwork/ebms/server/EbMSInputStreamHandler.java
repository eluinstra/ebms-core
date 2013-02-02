package nl.clockwork.ebms.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.clockwork.ebms.processor.EbMSProcessorException;

public interface EbMSInputStreamHandler
{
	void handle(InputStream request) throws EbMSProcessorException;
	void writeStatusCode(int statusCode);
	void writeHeader(String name, String value);
	OutputStream getOutputStream() throws IOException;
}
