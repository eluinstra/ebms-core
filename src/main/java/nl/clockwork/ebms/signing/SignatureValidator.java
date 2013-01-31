package nl.clockwork.ebms.signing;

import java.util.List;

import nl.clockwork.ebms.model.EbMSAttachment;

import org.w3c.dom.Document;

public interface SignatureValidator
{
	boolean validateMessage(Document document, List<EbMSAttachment> attachments) throws Exception;
}
