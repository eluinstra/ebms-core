/**
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
package nl.clockwork.ebms.client.apache;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageReader;
import nl.clockwork.ebms.client.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSResponseHandler implements ResponseHandler<EbMSDocument>
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);

	@Override
	public EbMSDocument handleResponse(HttpResponse response) throws ClientProtocolException, IOException
	{
		try
		{
			if (response.getStatusLine().getStatusCode() / 100 == 2)
			{
				val entity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == HttpServletResponse.SC_NO_CONTENT || entity == null || entity.getContentLength() == 0)
				{
					messageLog.info("<<<<\nStatusCode=" + response.getStatusLine().getStatusCode());
					return null;
				}
				else
				{
					try (val input = entity.getContent())
					{
						val messageReader = new EbMSMessageReader(getHeaderField(response,"Content-ID"),getHeaderField(response,"Content-Type"));
						val message = IOUtils.toString(input,getEncoding(entity));
		      	messageLog.info("<<<<\nStatusCode=" + response.getStatusLine().getStatusCode() + "\n" + message);
						return messageReader.readResponse(message);
					}
				}
			}
			else if (response.getStatusLine().getStatusCode() >= HttpServletResponse.SC_BAD_REQUEST)
			{
		    val entity = response.getEntity();
		    if (entity != null)
					throw new IOException("StatusCode=" + response.getStatusLine().getStatusCode() + "\n" + IOUtils.toString(entity.getContent(),Charset.defaultCharset()));
			}
			throw new IOException("StatusCode=" + response.getStatusLine().getStatusCode());
		}
		catch (ParserConfigurationException | SAXException | EbMSProcessingException e)
		{
			throw new IOException(e);
		}
	}

	private String getEncoding(HttpEntity entity) throws EbMSProcessingException
	{
		val contentType = entity.getContentType().getValue();
		if (!StringUtils.isEmpty(contentType))
			return HTTPUtils.getCharSet(contentType);
		else
			throw new EbMSProcessingException("HTTP header Content-Type is not set!");
	}
	
	private String getHeaderField(HttpResponse response, String name)
	{
		Header result = response.getFirstHeader(name);
		return result != null ? result.getValue() : null;
	}
}
