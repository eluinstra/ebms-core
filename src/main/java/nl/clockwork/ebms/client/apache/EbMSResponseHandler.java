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
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.server.EbMSMessageReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

public class EbMSResponseHandler implements ResponseHandler<EbMSDocument>
{
  protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public EbMSDocument handleResponse(HttpResponse response) throws ClientProtocolException, IOException
	{
		try
		{
			if (response.getStatusLine().getStatusCode() / 100 == 2)
			{
				HttpEntity entity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == Constants.SC_NOCONTENT || entity == null || entity.getContentLength() == 0)
				{
					logger.info("<<<< statusCode = " + response.getStatusLine().getStatusCode());
					return null;
				}
				else
				{
					InputStream input = entity.getContent();
					EbMSMessageReader messageReader = new EbMSMessageReader(getHeaderField(response,"Content-ID"),getHeaderField(response,"Content-Type"));
					//EbMSDocument result = messageReader.read(input);
					EbMSDocument result = messageReader.readResponse(input,getEncoding(entity));
					if (logger.isInfoEnabled())
						logger.info("<<<< statusCode = " + response.getStatusLine().getStatusCode() + (result == null || result.getMessage() == null ? "" : "\n" + DOMUtils.toString(result.getMessage())));
					return result;
				}
			}
			else if (response.getStatusLine().getStatusCode() >= Constants.SC_BAD_REQUEST)
			{
		    HttpEntity entity = response.getEntity();
		    if (entity != null)
					throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode() + "\n" + IOUtils.toString(entity.getContent()));
			}
			throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode());
		}
		catch (ParserConfigurationException | SAXException | TransformerException | EbMSProcessingException e)
		{
			throw new IOException(e);
		}
	}

	private String getEncoding(HttpEntity entity) throws EbMSProcessingException
	{
		String contentType = entity.getContentType().getValue();
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
