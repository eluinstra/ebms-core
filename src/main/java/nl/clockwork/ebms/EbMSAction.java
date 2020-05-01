package nl.clockwork.ebms;

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSAction
{
	MESSAGE_ERROR("MessageError"),
	ACKNOWLEDGMENT("Acknowledgment"),
	STATUS_REQUEST("StatusRequest"),
	STATUS_RESPONSE("StatusResponse"),
	PING("Ping"),
	PONG("Pong");

	String action;

	public static Stream<EbMSAction> stream()
	{
		return Stream.of(EbMSAction.values());
	}
	
	public static final EbMSAction get(String action)
	{
		return EbMSAction.stream().filter(a -> a.action.equals(action)).findFirst().orElse(null);
	}

}