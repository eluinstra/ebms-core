package nl.clockwork.ebms.event.processor;

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSEventStatus
{
	SUCCEEDED(1), FAILED(2), EXPIRED(3);

	int id;

	public static Stream<EbMSEventStatus> stream()
	{
		return Stream.of(EbMSEventStatus.values());
	}

	public static final EbMSEventStatus get(int id)
	{
		return EbMSEventStatus.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
	}
}