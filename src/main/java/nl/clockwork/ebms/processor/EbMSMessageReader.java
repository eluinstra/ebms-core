package nl.clockwork.ebms.processor;

import java.io.InputStream;

import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.RawEbMSMessage;

public interface EbMSMessageReader
{
	RawEbMSMessage read(String contentType, InputStream in) throws EbMSProcessorException;
	EbMSMessage read(RawEbMSMessage message) throws EbMSProcessorException;
}
