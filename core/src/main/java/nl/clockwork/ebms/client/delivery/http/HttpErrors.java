/*
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.clockwork.ebms.client.delivery.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
class HttpErrors
{
	@NonNull
	List<Integer> recoverableHttpErrors;
	@NonNull
	List<Integer> unrecoverableHttpErrors;

	public HttpErrors(
			String recoverableInformationalHttpErrors,
			String recoverableRedirectionHttpErrors,
			String recoverableClientHttpErrors,
			String unrecoverableServerHttpErrors)
	{
		this(
				getIntegerList(recoverableInformationalHttpErrors),
				getIntegerList(recoverableRedirectionHttpErrors),
				getIntegerList(recoverableClientHttpErrors),
				getIntegerList(unrecoverableServerHttpErrors));
	}

	public HttpErrors(
			@NonNull List<Integer> recoverableInformationalHttpErrors,
			@NonNull List<Integer> recoverableRedirectionHttpErrors,
			@NonNull List<Integer> recoverableClientHttpErrors,
			@NonNull List<Integer> unrecoverableServerHttpErrors)
	{
		val recoverableCodes = new ArrayList<Integer>();
		recoverableCodes.addAll(recoverableInformationalHttpErrors);
		recoverableCodes.addAll(recoverableRedirectionHttpErrors);
		recoverableCodes.addAll(recoverableClientHttpErrors);
		this.recoverableHttpErrors = Collections.unmodifiableList(recoverableCodes);
		this.unrecoverableHttpErrors = Collections.unmodifiableList(unrecoverableServerHttpErrors);
	}

	private static List<Integer> getIntegerList(String input)
	{
		return Arrays.stream(StringUtils.split(input, ',')).map(s -> Integer.parseInt(s.trim())).toList();
	}
}
