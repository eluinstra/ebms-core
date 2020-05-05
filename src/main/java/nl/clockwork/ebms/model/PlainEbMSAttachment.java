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

import javax.activation.DataSource;

import org.apache.commons.io.IOUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class PlainEbMSAttachment implements EbMSAttachment
{
	@Getter
	String contentId;
	@NonNull
	DataSource dataSource;
	
	@Override
	public String getContentType()
	{
		return dataSource.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return dataSource.getInputStream();
	}

	@Override
	public String getName()
	{
		return dataSource.getName();
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return dataSource.getOutputStream();
	}

	@Override
	public void writeTo(OutputStream outputStream) throws IOException
	{
		IOUtils.copy(getInputStream(),outputStream);
	}

	@Override
	public void close()
	{
	}

}
