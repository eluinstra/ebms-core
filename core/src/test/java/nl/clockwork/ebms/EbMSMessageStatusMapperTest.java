package nl.clockwork.ebms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

import nl.clockwork.ebms.common.EbMSMessageStatus;

class EbMSMessageStatusMapperTest
{
  @ParameterizedTest
  @MethodSource("testToMessageStatusType")
  void testToMessageStatusType(EbMSMessageStatus status, MessageStatusType expected)
  {
    assertThat(EbMSMessageStatusMapper.toMessageStatusType(status)).isEqualTo(expected);
  }

	static Stream<Arguments> testToMessageStatusType()
  {
    return Stream.of(
      of(EbMSMessageStatus.UNAUTHORIZED, MessageStatusType.UN_AUTHORIZED),
      of(EbMSMessageStatus.NOT_RECOGNIZED, MessageStatusType.NOT_RECOGNIZED),
      of(EbMSMessageStatus.RECEIVED, MessageStatusType.RECEIVED),
      of(EbMSMessageStatus.PROCESSED, MessageStatusType.PROCESSED),
      of(EbMSMessageStatus.FORWARDED, MessageStatusType.FORWARDED),
      of(EbMSMessageStatus.FAILED, MessageStatusType.RECEIVED),
      of(EbMSMessageStatus.CREATED, null),
      of(EbMSMessageStatus.DELIVERY_FAILED, null),
      of(EbMSMessageStatus.DELIVERED, null),
      of(EbMSMessageStatus.EXPIRED, null)
    );
  }

  @ParameterizedTest
  @MethodSource("testToEbMSMessageStatus")
  void testToEbMSMessageStatus(MessageStatusType status, Optional<EbMSMessageStatus> expected)
  {
    assertThat(EbMSMessageStatusMapper.toEbMSMessageStatus(status)).isEqualTo(expected);
  }

  static Stream<Arguments> testToEbMSMessageStatus()
  {
    return Stream.of(
      of(MessageStatusType.UN_AUTHORIZED, Optional.of(EbMSMessageStatus.UNAUTHORIZED)),
      of(MessageStatusType.NOT_RECOGNIZED, Optional.of(EbMSMessageStatus.NOT_RECOGNIZED)),
      of(MessageStatusType.RECEIVED, Optional.of(EbMSMessageStatus.RECEIVED)),
      of(MessageStatusType.PROCESSED, Optional.of(EbMSMessageStatus.PROCESSED)),
      of(MessageStatusType.FORWARDED, Optional.of(EbMSMessageStatus.FORWARDED))
      // of(MessageStatusType.RECEIVED, Optional.of(EbMSMessageStatus.FAILED))
    );
  }
}
