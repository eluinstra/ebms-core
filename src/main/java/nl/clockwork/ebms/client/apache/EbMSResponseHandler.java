/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.client.apache;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EbMSResponseHandler implements ResponseHandler<EbMSDocument>
{

	@Override
	public EbMSDocument handleResponse(HttpResponse response) throws ClientProtocolException, IOException
	{
		EbMSDocument in = null;
		if (response.getStatusLine().getStatusCode() / 100 == 2)
		{
	    HttpEntity entity = response.getEntity();
	    if (entity != null && entity.getContentLength() != 0)
	    {
				InputStream content = entity.getContent();
				try
				{
					in = getEbMSMessage(IOUtils.toString(content));
				}
				catch (ParserConfigurationException e)
				{
					throw new IOException(e);
				}
				catch (SAXException e)
				{
					throw new IOException(e);
				}
	    	finally
	    	{
	    		try
					{
						content.close();
					}
					catch (IOException ignore)
					{
					}
	    	}
	    }
		}
		else if (response.getStatusLine().getStatusCode() >= 400)
		{
	    HttpEntity entity = response.getEntity();
	    if (entity != null)
	    {
				InputStream content = entity.getContent();
				try
				{
					throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode() + "\n" + IOUtils.toString(content));
				}
	    	finally
	    	{
	    		try
					{
						content.close();
					}
					catch (IOException ignore)
					{
					}
	    	}
	    }
		}
		else
			throw new IOException("StatusCode: " + response.getStatusLine().getStatusCode());
    return in;
	}

	private EbMSDocument getEbMSMessage(String message) throws ParserConfigurationException, SAXException, IOException
	{
		EbMSDocument result = null;
		if (StringUtils.isNotBlank(message))
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse(new InputSource(new StringReader(message)));
			result = new EbMSDocument(d,new ArrayList<EbMSAttachment>());
		}
		return result;
	}

}
