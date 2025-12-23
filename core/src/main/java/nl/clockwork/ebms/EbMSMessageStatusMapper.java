package nl.clockwork.ebms;

import java.util.Optional;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

import nl.clockwork.ebms.common.EbMSMessageStatus;

public class EbMSMessageStatusMapper {
  
  private static MessageStatusType[] ebMSMessageStatusMapping = {
    MessageStatusType.UN_AUTHORIZED,
    MessageStatusType.NOT_RECOGNIZED,
    MessageStatusType.RECEIVED,
    MessageStatusType.PROCESSED,
    MessageStatusType.FORWARDED,
    MessageStatusType.RECEIVED, // for FAILED
    null, // for 6
    null, // for 7
    null, // for 8
    null, // for 9
    null, // for CREATED
    null, // for DELIVERY_FAILED
    null, // for DELIVERED
    null  // for EXPIRED
  };

  private static EbMSMessageStatus[] messageStatusTypeMapping = {
    EbMSMessageStatus.UNAUTHORIZED,
    EbMSMessageStatus.NOT_RECOGNIZED,
    EbMSMessageStatus.RECEIVED,
    EbMSMessageStatus.PROCESSED,
    EbMSMessageStatus.FORWARDED,
    EbMSMessageStatus.FAILED,
  };

  public static MessageStatusType toMessageStatusType(EbMSMessageStatus status) {
    return ebMSMessageStatusMapping[status.getId()];
  }

	public static Optional<EbMSMessageStatus> toEbMSMessageStatus(MessageStatusType statusCode)
	{
		return Optional.ofNullable(messageStatusTypeMapping[statusCode.ordinal()]);
	}
}
