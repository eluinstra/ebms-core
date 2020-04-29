package nl.clockwork.ebms;

import java.util.stream.Stream;

public enum EbMSMessageEventType
{
	RECEIVED,DELIVERED,FAILED,EXPIRED;
	
	public static Stream<EbMSMessageEventType> stream()
	{
		return Stream.of(EbMSMessageEventType.values());
	}
}