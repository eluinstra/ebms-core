package nl.clockwork.ebms.signing;

import java.util.List;

import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.Signature;

import org.w3c.dom.Document;

public interface EbMSSignatureValidator
{
	Signature validateSignature(Document document, List<EbMSAttachment> attachments) throws Exception;
}
