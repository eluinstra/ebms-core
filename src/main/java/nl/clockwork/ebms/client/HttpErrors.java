package nl.clockwork.ebms.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public class HttpErrors
{
	@NonNull
	List<Integer> recoverableHttpErrors;
	@NonNull
	List<Integer> unrecoverableHttpErrors;

	public HttpErrors(String recoverableInformationalHttpErrors, String recoverableRedirectionHttpErrors, String recoverableClientHttpErrors, String unrecoverableServerHttpErrors)
	{
		this(getIntegerList(recoverableInformationalHttpErrors),getIntegerList(recoverableRedirectionHttpErrors),getIntegerList(recoverableClientHttpErrors),getIntegerList(unrecoverableServerHttpErrors));
	}

	public HttpErrors(
			@NonNull List<Integer> recoverableInformationalHttpErrors,
			@NonNull List<Integer> recoverableRedirectionHttpErrors,
			@NonNull List<Integer> recoverableClientHttpErrors,
			@NonNull List<Integer> unrecoverableServerHttpErrors)
	{
		val recoverableHttpErrors = new ArrayList<Integer>();
		recoverableHttpErrors.addAll(recoverableInformationalHttpErrors);
		recoverableHttpErrors.addAll(recoverableRedirectionHttpErrors);
		recoverableHttpErrors.addAll(recoverableClientHttpErrors);
		this.recoverableHttpErrors = Collections.unmodifiableList(recoverableHttpErrors);
		this.unrecoverableHttpErrors = Collections.unmodifiableList(unrecoverableServerHttpErrors);
	}

	private static List<Integer> getIntegerList(String input)
	{
		return Arrays.stream(StringUtils.split(input,',')).map(s -> Integer.parseInt(s.trim())).collect(Collectors.toList());
	}
}
