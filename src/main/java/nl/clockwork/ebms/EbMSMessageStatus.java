package nl.clockwork.ebms;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

	private static final Collection<EbMSMessageStatus> RECEIVESTATUS =
			Collections.unmodifiableCollection(Arrays.asList(UNAUTHORIZED,NOT_RECOGNIZED,RECEIVED,PROCESSED,FORWARDED,FAILED));
	private static final Collection<EbMSMessageStatus> SENDSTATUS =
			Collections.unmodifiableCollection(Arrays.asList(SENDING,DELIVERY_FAILED,DELIVERED,EXPIRED));
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

	public static final Collection<EbMSMessageStatus> getReceiveStatus()
	{
		return RECEIVESTATUS;
	}

	public static final Collection<EbMSMessageStatus> getSendStatus()
	{
		return SENDSTATUS;
	}
}