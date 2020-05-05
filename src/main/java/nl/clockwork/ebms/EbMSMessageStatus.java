package nl.clockwork.ebms;

import java.util.Arrays;
import java.util.stream.Stream;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Getter
public enum EbMSMessageStatus
{
	UNAUTHORIZED(0,MessageStatusType.UN_AUTHORIZED),
	NOT_RECOGNIZED(1,MessageStatusType.NOT_RECOGNIZED),
	RECEIVED(2,MessageStatusType.RECEIVED),
	PROCESSED(3,MessageStatusType.PROCESSED),
	FORWARDED(4,MessageStatusType.FORWARDED),
	FAILED(5,MessageStatusType.RECEIVED),
	/*WAITING(6,MessageStatusType.RECEIVED),*/
	SENDING(10),
	DELIVERY_FAILED(11),
	DELIVERED(12),
	EXPIRED(13);

	private static final EbMSMessageStatus[] RECEIVE_STATUS = {UNAUTHORIZED,NOT_RECOGNIZED,RECEIVED,PROCESSED,FORWARDED,FAILED};
	private static final EbMSMessageStatus[] SEND_STATUS = {SENDING,DELIVERY_FAILED,DELIVERED,EXPIRED};
	int id;
	@NonFinal
	MessageStatusType statusCode = null;

	public static Stream<EbMSMessageStatus> stream()
	{
		return Stream.of(EbMSMessageStatus.values());
	}

	public static final EbMSMessageStatus get(int id)
	{
		return EbMSMessageStatus.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
		//orElseThrow(() -> new IllegalStateException("Unsupported EbMSMessageStatus Id: " + id));
	}

	public static final EbMSMessageStatus get(String name)
	{
		return EbMSMessageStatus.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
	}

	public static final EbMSMessageStatus get(MessageStatusType statusCode)
	{
		return EbMSMessageStatus.stream().filter(s -> s.statusCode.equals(statusCode)).findFirst().orElse(null);
	}

	public static final EbMSMessageStatus[] getReceiveStatus()
	{
		return Arrays.copyOf(RECEIVE_STATUS,RECEIVE_STATUS.length);
	}

	public static final EbMSMessageStatus[] getSendStatus()
	{
		return Arrays.copyOf(SEND_STATUS,SEND_STATUS.length);
	}
}