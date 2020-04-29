package nl.clockwork.ebms;

import java.util.stream.Stream;

public enum EbMSEventStatus
{
	SUCCEEDED(1), FAILED(2), EXPIRED(3);

	private final int id;

	EbMSEventStatus(int id)
	{
		this.id = id;
	}

	public final int id()
	{
		return id;
	}

	public static Stream<EbMSEventStatus> stream()
	{
		return Stream.of(EbMSEventStatus.values());
	}

	public static final EbMSEventStatus get(int id)
	{
		return EbMSEventStatus.stream().filter(s -> s.id() == id).findFirst().orElse(null);
	}
}