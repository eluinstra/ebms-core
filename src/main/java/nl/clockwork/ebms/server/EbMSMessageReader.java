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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSMessageReader
{
	private String contentId;
	private String contentType;
	
	public EbMSMessageReader(String contentId, String contentType)
	{
		this.contentId = contentId;
		this.contentType = contentType;
	}

	public EbMSDocument read(InputStream in) throws MimeException, IOException, ParserConfigurationException, SAXException
	{
		
		if (contentType.startsWith("multipart"))
		{
			EbMSContentHandler handler = new EbMSContentHandler();
			parseEbMSMessage(handler,contentType,in);
			List<EbMSAttachment> attachments = handler.getAttachments();
			return getEbMSMessage(attachments);
		}
		else
			return getEbMSMessage(in);
	}

	public EbMSDocument readResponse(InputStream in, String encoding) throws IOException, ParserConfigurationException, SAXException
	{
		EbMSDocument result = null;
		String message = IOUtils.toString(in,encoding);
		if (StringUtils.isNotBlank(message))
		{
			Document d = DOMUtils.read(message);
			result = new EbMSDocument(contentId,d,new ArrayList<>());
		}
		return result;
	}

	private void parseEbMSMessage(EbMSContentHandler handler, String contentType, InputStream in) throws MimeException, IOException
	{
		MimeConfig mimeConfig = MimeConfig.custom().setHeadlessParsing(contentType).build();
	  MimeStreamParser parser = new MimeStreamParser(mimeConfig);
	  parser.setContentHandler(handler);
		parser.parse(in);
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws ParserConfigurationException, SAXException, IOException
	{
		Document d = DOMUtils.read(in);
		return new EbMSDocument(contentId,d,new ArrayList<>());
	}

	private EbMSDocument getEbMSMessage(List<EbMSAttachment> attachments) throws ParserConfigurationException, SAXException, IOException
	{
		EbMSDocument result = null;
		if (attachments.size() > 0)
		{
			Document d = DOMUtils.read((attachments.get(0).getInputStream()));
			attachments.remove(0);
			result = new EbMSDocument(contentId,d,attachments);
		}
		return result;
	}

}
