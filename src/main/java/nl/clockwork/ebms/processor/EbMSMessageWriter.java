package nl.clockwork.ebms.processor;

import nl.clockwork.ebms.model.EbMSDocument;

public interface EbMSMessageWriter
{
	void write(EbMSDocument document) throws EbMSProcessorException;
	void flush() throws EbMSProcessorException;
}
