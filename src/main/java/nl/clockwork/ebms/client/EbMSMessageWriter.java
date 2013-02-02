package nl.clockwork.ebms.client;

import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

public interface EbMSMessageWriter
{
	void write(EbMSDocument document) throws EbMSProcessorException;
	void flush() throws EbMSProcessorException;
}
