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


import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.DOMUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSResponseHandler
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	HttpResponse<String> response;
	@NonNull
	List<Integer> recoverableHttpErrors;
	@NonNull
	List<Integer> unrecoverableHttpErrors;

	public EbMSDocument read() throws EbMSProcessorException
	{
		try
		{
			switch (response.statusCode() / 100)
			{
				case 2:
					if (response.statusCode() == HttpServletResponse.SC_NO_CONTENT || response.body().length() == 0)
					{
						logResponse(response);
						return null;
					}
					else
						return readSuccesResponse(response);
				case 1:
				case 3:
				case 4:
					throw createRecoverableErrorException(response);
				case 5:
					throw createUnrecoverableErrorException(response);
				default:
					logResponse(response);
					throw new EbMSUnrecoverableResponseException(response);
			}
		}
		catch (IOException e)
		{
			throw new EbMSResponseException(response, e);
		}
	}

	private EbMSDocument readSuccesResponse(HttpResponse<String> response) throws IOException
	{
		logResponse(response);
		try
		{
			return toEbMSDocument(response.headers().firstValue("Content-ID").orElse(null), response.body());
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSResponseException(response, e);
		}
	}

	public EbMSDocument toEbMSDocument(String contentId, String message) throws IOException, ParserConfigurationException, SAXException
	{
		return StringUtils.isNotBlank(message)
				? EbMSDocument.builder().contentId(contentId).message(DOMUtils.read(message)).attachments(Collections.emptyList()).build()
				: null;
	}

	private EbMSResponseException createRecoverableErrorException(HttpResponse<String> response)
	{
		return recoverableHttpErrors.contains(response.statusCode()) ? new EbMSResponseException(response) : new EbMSUnrecoverableResponseException(response);
	}

	private EbMSResponseException createUnrecoverableErrorException(HttpResponse<String> response)
	{
		return unrecoverableHttpErrors.contains(response.statusCode()) ? new EbMSUnrecoverableResponseException(response) : new EbMSResponseException(response);
	}

	private void logResponse(HttpResponse<String> response)
	{
		messageLog.info("<<<<\nStatusCode={}\nHeaders={}{}", response.statusCode(), response.headers(), (response.body() != null ? "\n" + response.body() : ""));
	}
}
