package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.SignatureType;

public abstract class EbMSMessageResponse extends EbMSResponseMessage
{
	public EbMSMessageResponse(MessageHeader messageHeader, SignatureType signature)
	{
		super(messageHeader,signature);
	}
}
