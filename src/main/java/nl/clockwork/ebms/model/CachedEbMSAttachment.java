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
package nl.clockwork.ebms.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.cxf.io.CachedOutputStream;

public class CachedEbMSAttachment implements EbMSAttachment
{
	private String name;
	private String contentId;
	private String contentType;
	private CachedOutputStream content;

	public CachedEbMSAttachment(String name, String contentId, String contentType, CachedOutputStream content)
	{
		this.name = name;
		this.contentId = contentId;
		this.contentType = contentType;
		this.content = content;
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return content.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return content;//.getOut();
	}

	@Override
	public String getContentType()
	{
		return contentType;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getContentId()
	{
		return contentId;
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException
	{
		content.writeCacheTo(outputStream);
	}

	@Override
	public void close()
	{
		try
		{
			CachedOutputStream c = content;
			content = null;
			c.close();
		}
		catch (IOException e)
		{
			//do nothing
		}
	}
}
