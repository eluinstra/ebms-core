package nl.clockwork.ebms.signing;

import java.util.List;

import nl.clockwork.ebms.model.EbMSAttachment;

import org.w3c.dom.Document;

public interface EbMSSignatureGenerator
{
	Document generateSignature(Document d, List<EbMSAttachment> attachments) throws Exception;
}