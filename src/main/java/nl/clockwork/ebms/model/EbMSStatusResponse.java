package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSStatusResponse extends EbMSResponseMessage
{
	@NonNull
	StatusResponse statusResponse;

	@Builder
	public EbMSStatusResponse(MessageHeader messageHeader, SignatureType signature, @NonNull StatusResponse statusResponse)
	{
		super(messageHeader,signature);
		this.statusResponse = statusResponse;
	}
}
