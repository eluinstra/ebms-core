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
package nl.clockwork.ebms.delivery.client;


import java.net.http.HttpResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.processor.EbMSProcessingException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public class EbMSResponseException extends EbMSProcessingException
{
	private static final long serialVersionUID = 1L;
	HttpResponse<String> repsonse;

	public EbMSResponseException(@NonNull HttpResponse<String> repsonse, String message)
	{
		super(message);
		this.repsonse = repsonse;
	}

	public EbMSResponseException(@NonNull HttpResponse<String> repsonse, Throwable cause)
	{
		super(cause);
		this.repsonse = repsonse;
	}

	public EbMSResponseException(@NonNull HttpResponse<String> repsonse, String message, Throwable cause)
	{
		super(message, cause);
		this.repsonse = repsonse;
	}

	@Override
	public String getMessage()
	{
		return "StatusCode=" + repsonse.statusCode() + "\nHeaders=" + repsonse.headers().toString() + "\n" + (super.getMessage() != null ? super.getMessage() : "");
	}
}
