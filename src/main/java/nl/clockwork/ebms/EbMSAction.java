package nl.clockwork.ebms;

import java.util.stream.Stream;

public enum EbMSAction
{
	MESSAGE_ERROR("MessageError"),
	ACKNOWLEDGMENT("Acknowledgment"),
	STATUS_REQUEST("StatusRequest"),
	STATUS_RESPONSE("StatusResponse"),
	PING("Ping"),
	PONG("Pong");

	private final String action;

	EbMSAction(String action)
	{
		this.action = action;
	}

	public final String action()
	{
		return action;
	}
	
	public static Stream<EbMSAction> stream()
	{
		return Stream.of(EbMSAction.values());
	}
	
	public static final EbMSAction get(String action)
	{
		return EbMSAction.stream().filter(a -> a.action.equals(action)).findFirst().orElse(null);
	}

}