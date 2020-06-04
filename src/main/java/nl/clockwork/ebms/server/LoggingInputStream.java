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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class LoggingInputStream extends FilterInputStream
{
	transient Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	StringBuffer sb = new StringBuffer();
	@NonNull
	String charset;

	public LoggingInputStream(@NonNull InputStream in)
	{
		this(in,"UTF-8");
	}

	protected LoggingInputStream(@NonNull InputStream in, @NonNull String charset)
	{
		super(in);
		this.charset = charset;
	}

	@Override
	public int read() throws IOException
	{
		val result = super.read();
		if (messageLog.isDebugEnabled() && result != -1)
			sb.append(result);
		return result;
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		val len = super.read(b);
		if (messageLog.isDebugEnabled() && len != -1)
			sb.append(new String(b,0,len,charset));
		return len;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException
	{
		len = super.read(b,off,len);
		if (messageLog.isDebugEnabled() && len != -1)
			sb.append(new String(b,0,len,charset));
		return len;
	}
	
	@Override
	public void close() throws IOException
	{
		messageLog.debug("<<<<\n" + sb.toString());
		super.close();
	}
}
