package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.Builder;
import lombok.NonNull;

public class EbMSPing extends EbMSRequestMessage
{
	@Builder
	public EbMSPing(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply)
	{
		super(messageHeader,signature,syncReply);
	}
}
