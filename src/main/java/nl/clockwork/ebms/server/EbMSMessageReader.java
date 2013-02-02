package nl.clockwork.ebms.server;

import java.io.InputStream;

import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

public interface EbMSMessageReader
{
	EbMSDocument read(InputStream in) throws EbMSProcessorException;
}
