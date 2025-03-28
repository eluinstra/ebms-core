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
package nl.clockwork.ebms.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.cxf.io.CachedOutputStream;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CachedEbMSAttachment implements EbMSAttachment
{
	@Getter
	String name;
	@NonNull
	@Getter
	String contentId;
	@NonNull
	@Getter
	String contentType;
	@NonNull
	CachedOutputStream content;

	@Override
	public InputStream getInputStream() throws IOException
	{
		return CloseShieldInputStream.wrap(content.getInputStream());
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return CloseShieldOutputStream.wrap(content.getOut());
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
			content.close();
		}
		catch (IOException e)
		{
			// do nothing
		}
	}
}
