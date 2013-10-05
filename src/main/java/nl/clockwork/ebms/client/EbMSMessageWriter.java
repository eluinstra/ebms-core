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

public class EbMSMessageWriter
{
	private HttpURLConnection connection;
	
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

	private void writeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		connection.setRequestProperty("Content-Type","text/xml");
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);
		DOMUtils.write(document.getMessage(),connection.getOutputStream(),"UTF-8");
	}
	
	private void writeMimeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		String boundary = createBoundary();
		String contentType = createContentType(boundary);

		connection.setRequestProperty("MIME-Version","1.0");
		connection.setRequestProperty("Content-Type",contentType);
		connection.setRequestProperty("SOAPAction",Constants.EBMS_SOAP_ACTION);

		OutputStream outputStream = connection.getOutputStream();

		OutputStreamWriter writer = new OutputStreamWriter(outputStream,"UTF-8");

		writer.write("--");
		writer.write(boundary);
		writer.write("\r\n");

		writer.write("Content-Type: text/xml; charset=UTF-8");
		writer.write("\r\n");
		writer.write("Content-ID: <0>");
		writer.write("\r\n");
		writer.write("\r\n");
		DOMUtils.write(document.getMessage(),writer,"UTF-8");
		writer.write("\r\n");
		writer.write("--");
		writer.write(boundary);

		for (EbMSAttachment attachment : document.getAttachments())
		{
			writer.write("\r\n");
			writer.write("Content-Type: " + attachment.getContentType());
			writer.write("\r\n");
			writer.write("Content-Transfer-Encoding: binary");
			writer.write("\r\n");
			writer.write("Content-ID: <" + attachment.getContentId() + ">");
			writer.write("\r\n");
			writer.write("\r\n");
			writer.flush();
			IOUtils.copy(attachment.getDataSource().getInputStream(),outputStream);
			writer.write("\r\n");
			writer.write("--");
			writer.write(boundary);
		}
	
		writer.write("--");
		writer.close();
	}

	private String createBoundary()
	{
		return "-=Part.0." + UUID.randomUUID() + "=-";
	}

	private String createContentType(String boundary)
	{
		return "multipart/related; boundary=\"" + boundary + "\"; type=\"text/xml\"; start=\"<0>\"; start-info=\"text/xml\"";
	}

}
