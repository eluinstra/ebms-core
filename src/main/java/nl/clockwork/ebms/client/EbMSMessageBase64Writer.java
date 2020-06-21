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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

import org.apache.james.mime4j.codec.Base64OutputStream;
import org.springframework.util.StringUtils;

import lombok.val;
import nl.clockwork.ebms.model.EbMSAttachment;

class EbMSMessageBase64Writer extends EbMSMessageWriter
{
	public EbMSMessageBase64Writer(HttpURLConnection connection)
	{
		super(connection);
	}

	protected void writeBinaryAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		try (val a = attachment)
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
			val out = new Base64OutputStream(outputStream);
			a.writeTo(out);
			out.flush();
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);
		}
	}
}
