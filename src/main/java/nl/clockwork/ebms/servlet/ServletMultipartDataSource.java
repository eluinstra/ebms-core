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
package nl.clockwork.ebms.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;

import javax.activation.DataSource;
import javax.servlet.ServletRequest;

public class ServletMultipartDataSource implements DataSource
{
	String contentType;
	InputStream inputStream;

	public ServletMultipartDataSource(ServletRequest request) throws IOException
	{
		inputStream = new SequenceInputStream(new ByteArrayInputStream("\n".getBytes()),request.getInputStream());
		contentType = request.getContentType();
	}

	public InputStream getInputStream() throws IOException
	{
		return inputStream;
	}

	public OutputStream getOutputStream() throws IOException
	{
		return null;
	}

	public String getContentType()
	{
		return contentType;
	}

	public String getName()
	{
		return "ServletMultipartDataSource";
	}
}
