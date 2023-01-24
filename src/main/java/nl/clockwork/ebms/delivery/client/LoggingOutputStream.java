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
package nl.clockwork.ebms.delivery.client;


import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class LoggingOutputStream extends FilterOutputStream
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	Map<String, List<String>> properties;
	@NonNull
	String charset;
	StringBuffer sb = new StringBuffer();

	public LoggingOutputStream(Map<String, List<String>> properties, OutputStream out)
	{
		this(properties, out, "UTF-8");
	}

	public LoggingOutputStream(@NonNull Map<String, List<String>> properties, @NonNull OutputStream out, @NonNull String charset)
	{
		super(out);
		this.properties = properties;
		this.charset = charset;
	}

	@Override
	public void write(int b) throws IOException
	{
		if (messageLog.isDebugEnabled())
			sb.append(b);
		out.write(b);
	}

	@Override
	public void write(byte[] b) throws IOException
	{
		if (messageLog.isDebugEnabled())
			sb.append(new String(b, charset));
		out.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		if (messageLog.isDebugEnabled())
			sb.append(new String(b, off, len, charset));
		out.write(b, off, len);
	}

	@Override
	public void close() throws IOException
	{
		messageLog.debug(">>>>\n" + HTTPUtils.toString(this.properties) + "\n" + sb.toString());
		super.close();
	}

}