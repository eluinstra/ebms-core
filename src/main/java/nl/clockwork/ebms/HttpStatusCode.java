package nl.clockwork.ebms;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum HttpStatusCode
{
	SC_OK(200), SC_NOCONTENT(204), SC_BAD_REQUEST(400), SC_INTERNAL_SERVER_ERROR(500);

	int code;
}
