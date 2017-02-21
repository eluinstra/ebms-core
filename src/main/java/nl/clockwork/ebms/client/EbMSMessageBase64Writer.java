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
package nl.clockwork.ebms.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import nl.clockwork.ebms.model.EbMSAttachment;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

public class EbMSMessageBase64Writer extends EbMSMessageWriter
{
	public EbMSMessageBase64Writer(HttpURLConnection connection)
	{
		super(connection);
	}

	protected void writeBinaryAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		writer.write("\r\n");
		writer.write("Content-Type: " + attachment.getContentType());
		writer.write("\r\n");
		if (!StringUtils.isEmpty(attachment.getName()))
		{
			writer.write("Content-Disposition: attachment; filename=\"" + attachment.getName() + "\"");
			writer.write("\r\n");
		}
		writer.write("Content-Transfer-Encoding: base64");
		writer.write("\r\n");
		writer.write("Content-ID: <" + attachment.getContentId() + ">");
		writer.write("\r\n");
		writer.write("\r\n");
		writer.flush();
		Base64InputStream base64InputStream = new Base64InputStream(attachment.getInputStream(),true);
		IOUtils.copy(base64InputStream,outputStream);
		writer.write("\r\n");
		writer.write("--");
		writer.write(boundary);
	}
}
