package nl.clockwork.ebms.processor;

import java.io.InputStream;

import nl.clockwork.ebms.model.EbMSDocument;

public interface EbMSMessageReader
{
	EbMSDocument read(InputStream in) throws EbMSProcessorException;
}
