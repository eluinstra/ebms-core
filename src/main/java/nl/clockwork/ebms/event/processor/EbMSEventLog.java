package nl.clockwork.ebms.event.processor;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(staticName = "of")
@Getter
public class EbMSEventLog
{
	@NonNull
	String messageId;
	@NonNull
	Instant timestamp;
	@NonNull
	String uri;
	@NonNull
	EbMSEventStatus status;
	String errorMessage;
}
