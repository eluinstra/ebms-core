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
package nl.clockwork.ebms.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.util.ByteArrayDataSource;

import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;

public class EbMSContentHandler implements ContentHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Map<String,String> headers = new HashMap<String,String>();
	private List<EbMSAttachment> attachments = new ArrayList<EbMSAttachment>();

	@Override
	public void startMessage() throws MimeException
	{
	}

	@Override
	public void endMessage() throws MimeException
	{
	}

	@Override
	public void startBodyPart() throws MimeException
	{
	}

	@Override
	public void endBodyPart() throws MimeException
	{
	}

	@Override
	public void startHeader() throws MimeException
	{
	}

	@Override
	public void field(Field rawField) throws MimeException
	{
		headers.put(rawField.getName(),rawField.getBody());
	}

	@Override
	public void endHeader() throws MimeException
	{
	}

	@Override
	public void preamble(InputStream is) throws MimeException, IOException
	{
	}

	@Override
	public void epilogue(InputStream is) throws MimeException, IOException
	{
	}

	@Override
	public void startMultipart(BodyDescriptor bd) throws MimeException
	{
	}

	@Override
	public void endMultipart() throws MimeException
	{
	}

	@Override
	public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException
	{
		String contentType = getHeader("Content-Type");
		if (getHeader("Content-Transfer-Encoding").equalsIgnoreCase("base64"))
			is = new Base64InputStream(is);
		ByteArrayDataSource ds = new ByteArrayDataSource(is,contentType);
		//ds.setName("");
		String contentId = getHeader("Content-ID");
		if (contentId != null)
			contentId = contentId.replaceAll("^<(.*)>$|^(.*)$","$1$2");
		attachments.add(new EbMSAttachment(ds,contentId));
		headers.clear();
	}

	@Override
	public void raw(InputStream is) throws MimeException, IOException
	{
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}

	private String getHeader(String headerName)
	{
		String result = headers.get(headerName);
		if (result == null)
			for (String key : headers.keySet())
				if (headerName.equalsIgnoreCase(key))
				{
					result = headers.get(key);
					break;
				}
		return result;
	}

}
