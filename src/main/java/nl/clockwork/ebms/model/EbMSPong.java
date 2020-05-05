package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.Builder;
import lombok.NonNull;

public class EbMSPong extends EbMSResponseMessage
{
	@Builder
	public EbMSPong(@NonNull MessageHeader messageHeader, SignatureType signature)
	{
		super(messageHeader,signature);
	}
}
