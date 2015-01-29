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

import nl.clockwork.ebms.common.util.HTTPUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.server.EbMSMessageReader;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.xml.sax.SAXException;

public class EbMSResponseHandler implements ResponseHandler<EbMSDocument>
{

	@Override
	public EbMSDocument handleResponse(HttpResponse response) throws ClientProtocolException, IOException
	{
		try
		{
			if (response.getStatusLine().getStatusCode() / 100 == 2)
			{
				HttpEntity entity = response.getEntity();
				if (response.getStatusLine().getStatusCode() == 204 || entity == null || entity.getContentLength() == 0)
					return null;
				else
				{
					try (InputStream input = entity.getContent())
					{
						EbMSMessageReader messageReader = new EbMSMessageReader(getHeaderField(response,"Content-Type"));
						//return messageReader.read(input);
						return messageReader.readResponse(input,getEncoding(entity));
					}
				}
			}
			else if (response.getStatusLine().getStatusCode() >= 400)
			{
		    HttpEntity entity = response.getEntity();
		    if (entity != null)
					throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode() + "\n" + IOUtils.toString(entity.getContent()));
			}
			throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode());
		}
		catch (ParserConfigurationException | SAXException e)
		{
			throw new IOException(e);
		}
	}

	private String getEncoding(HttpEntity entity)
	{
		return HTTPUtils.getCharSet(entity.getContentType().getValue());
	}
	
	private String getHeaderField(HttpResponse response, String name)
	{
		Header result = response.getFirstHeader(name);
		if (result == null)
			for (Header header : response.getAllHeaders())
				if (header.getName().equalsIgnoreCase(name))
				{
					result = header;
					break;
				}
		return result.getValue();
	}
}
