package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public abstract class EbMSRequestMessage extends EbMSBaseMessage
{
	SyncReply syncReply;

	public EbMSRequestMessage(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply)
	{
		super(messageHeader,signature);
		this.syncReply = syncReply;
	}
}
