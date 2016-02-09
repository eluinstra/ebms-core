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

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

public class EbMSMessageWriter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	protected HttpURLConnection connection;
	
	public EbMSMessageWriter(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public void write(EbMSDocument document) throws IOException, TransformerException
	{
		if (document.getAttachments().size() > 0)
			writeMimeMessage(document);
		else
			writeMessage(document);
	}

	protected void writeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		if (logger.isInfoEnabled())
			logger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		connection.setRequestProperty("Content-Type","text/xml; charset=UTF-8");
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);
		//DOMUtils.write(document.getMessage(),logger.isInfoEnabled() ? new LoggingOutputStream(connection.getOutputStream()) : connection.getOutputStream(),"UTF-8");
		DOMUtils.write(document.getMessage(),connection.getOutputStream(),"UTF-8");
	}
	
	protected void writeMimeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		if (logger.isInfoEnabled() && !logger.isDebugEnabled())
			logger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		String boundary = createBoundary();
		String contentType = createContentType(boundary,document.getContentId());

		connection.setRequestProperty("MIME-Version","1.0");
		connection.setRequestProperty("Content-Type",contentType);
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);

		OutputStream outputStream = connection.getOutputStream();
		if (logger.isDebugEnabled())
			outputStream = new LoggingOutputStream(outputStream);

		try (OutputStreamWriter writer = new OutputStreamWriter(outputStream,"UTF-8"))
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

			for (EbMSAttachment attachment : document.getAttachments())
				if (attachment.getContentType().matches("^(text/.*|.*/xml)$"))
					writeTextAttachment(boundary,outputStream,writer,attachment);
				else
					writeBinaryAttachment(boundary,outputStream,writer,attachment);

			writer.write("--");
		}
	}

	private void writeTextAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		writer.write("\r\n");
		writer.write("Content-Type: " + attachment.getContentType());
		writer.write("\r\n");
		if (!StringUtils.isEmpty(attachment.getName()))
		{
			writer.write("Content-Disposition: attachment; filename=" + attachment.getName() + ";");
			writer.write("\r\n");
		}
		writer.write("Content-ID: <" + attachment.getContentId() + ">");
		writer.write("\r\n");
		writer.write("\r\n");
		writer.flush();
		IOUtils.copy(attachment.getInputStream(),outputStream);
		writer.write("\r\n");
		writer.write("--");
		writer.write(boundary);
	}

	private void writeBinaryAttachment(String boundary, OutputStream outputStream, OutputStreamWriter writer, EbMSAttachment attachment) throws IOException
	{
		writer.write("\r\n");
		writer.write("Content-Type: " + attachment.getContentType());
		writer.write("\r\n");
		if (!StringUtils.isEmpty(attachment.getName()))
		{
			writer.write("Content-Disposition: attachment; filename=" + attachment.getName() + ";");
			writer.write("\r\n");
		}
		writer.write("Content-Transfer-Encoding: binary");
		writer.write("\r\n");
		writer.write("Content-ID: <" + attachment.getContentId() + ">");
		writer.write("\r\n");
		writer.write("\r\n");
		writer.flush();
		IOUtils.copy(attachment.getInputStream(),outputStream);
		writer.write("\r\n");
		writer.write("--");
		writer.write(boundary);
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
