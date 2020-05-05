package nl.clockwork.ebms.model;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.SignatureType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSAcknowledgment extends EbMSResponseMessage
{
	@NonNull
	Acknowledgment acknowledgment;

	@Builder
	public EbMSAcknowledgment(MessageHeader messageHeader, SignatureType signature, @NonNull Acknowledgment acknowledgment)
	{
		super(messageHeader,signature);
		this.acknowledgment = acknowledgment;
	}
}
