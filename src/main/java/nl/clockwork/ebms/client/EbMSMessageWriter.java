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
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSMessageWriter
{
	transient Log messageLogger = LogFactory.getLog(Constants.MESSAGE_LOG);
	@NonNull
	HttpURLConnection connection;
	
	public void write(EbMSDocument document) throws IOException, TransformerException
	{
		if (document.getAttachments().size() > 0)
			writeMimeMessage(document);
		else
			writeMessage(document);
	}

	protected void writeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		connection.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);
		if (messageLogger.isInfoEnabled())
			messageLogger.info(">>>>\n" + (messageLogger.isDebugEnabled() ? HTTPUtils.toString(connection.getRequestProperties()) + "\n" : "") + DOMUtils.toString(document.getMessage()));
		//DOMUtils.write(document.getMessage(),messageLogger.isInfoEnabled() ? new LoggingOutputStream(connection.getOutputStream()) : connection.getOutputStream(),"UTF-8");
		DOMUtils.write(document.getMessage(),connection.getOutputStream(),"UTF-8");
	}
	
	protected void writeMimeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		if (messageLogger.isInfoEnabled() && !messageLogger.isDebugEnabled())
			messageLogger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		val boundary = createBoundary();
		String contentType = createContentType(boundary,document.getContentId());

		connection.setRequestProperty("Content-Type",contentType);
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);

		val requestProperties = connection.getRequestProperties();
		val outputStream = messageLogger.isDebugEnabled() ? new LoggingOutputStream(requestProperties,connection.getOutputStream()) : connection.getOutputStream();

		try (val writer = new OutputStreamWriter(outputStream,"UTF-8"))
		{
			writer.write("--");
			writer.write(boundary);
			writer.write("\r\n");

			writer.write("Content-Type: text/xml; charset=UTF-8");
			writer.write("\r\n");
			writer.write("Content-ID: <" + document.getContentId() + ">");
			writer.write("\r\n");
			writer.write("\r\n");
			DOMUtils.write(document.getMessage(),writer,"UTF-8");
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);

			for (val attachment: document.getAttachments())
			{
				if (attachment.getContentType().matches("^(text/.*|.*/xml)$"))
					writeTextAttachment(boundary,outputStream,writer,attachment);
				else
					writeBinaryAttachment(boundary,outputStream,writer,attachment);
			}

			writer.write("--");
		}
	}

	protected void writeTextAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		try (val a = attachment)
		{
			writer.write("\r\n");
			writer.write("Content-Type: " + a.getContentType());
			writer.write("\r\n");
			if (!StringUtils.isEmpty(a.getName()))
			{
				writer.write("Content-Disposition: attachment; filename=\"" + a.getName() + "\"");
				writer.write("\r\n");
			}
			writer.write("Content-ID: <" + a.getContentId() + ">");
			writer.write("\r\n");
			writer.write("\r\n");
			writer.flush();
			a.writeTo(outputStream);
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);
		}
	}

	protected void writeBinaryAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		try (val a = attachment)
		{
			writer.write("\r\n");
			writer.write("Content-Type: " + a.getContentType());
			writer.write("\r\n");
			if (!StringUtils.isEmpty(a.getName()))
			{
				writer.write("Content-Disposition: attachment; filename=\"" + a.getName() + "\"");
				writer.write("\r\n");
			}
			writer.write("Content-Transfer-Encoding: binary");
			writer.write("\r\n");
			writer.write("Content-ID: <" + a.getContentId() + ">");
			writer.write("\r\n");
			writer.write("\r\n");
			writer.flush();
			a.writeTo(outputStream);
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);
		}
	}

	protected String createBoundary()
	{
		return "-=" + UUID.randomUUID() + "=-";
	}

	protected String createContentType(String boundary, String contentId)
	{
		return "multipart/related; boundary=\"" + boundary + "\"; type=\"text/xml\"; start=\"<" + contentId + ">\"; start-info=\"text/xml\"";
	}

}
