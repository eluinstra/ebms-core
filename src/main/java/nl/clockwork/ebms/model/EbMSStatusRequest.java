package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSStatusRequest extends EbMSRequestMessage
{
	@NonNull
	StatusRequest statusRequest;

	@Builder
	public EbMSStatusRequest(@NonNull MessageHeader messageHeader, SignatureType signature, SyncReply syncReply, @NonNull StatusRequest statusRequest)
	{
		super(messageHeader,signature,syncReply);
		this.statusRequest = statusRequest;
	}
}
