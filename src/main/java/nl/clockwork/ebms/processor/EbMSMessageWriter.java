package nl.clockwork.ebms.processor;

import java.io.OutputStream;

import org.w3c.dom.Document;

import nl.clockwork.ebms.model.EbMSBaseMessage;

public interface EbMSMessageWriter
{
	Document write(EbMSBaseMessage ebMSMessage) throws EbMSProcessorException;
	void write(OutputStream out, Document message) throws EbMSProcessorException;
}
