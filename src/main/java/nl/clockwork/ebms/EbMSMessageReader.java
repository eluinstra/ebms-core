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
package nl.clockwork.ebms;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.XSDValidator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageReader
{
	String contentId;
	@NonNull
	String contentType;
	XSDValidator xsdValidator = new XSDValidator("/nl/clockwork/ebms/xsd/msg-header-2_0.xsd");
	
	public EbMSDocument read(InputStream in) throws MimeException, IOException, ParserConfigurationException, SAXException
	{
		
		if (contentType.startsWith("multipart"))
		{
			val handler = new EbMSContentHandler();
			parseEbMSMessage(handler,contentType,in);
			val attachments = handler.getAttachments();
			return getEbMSMessage(attachments);
		}
		else
			return getEbMSMessage(in);
	}

	public EbMSDocument readResponse(InputStream message) throws IOException, ParserConfigurationException, SAXException
	{
		xsdValidator.validate(message);
		message.reset();
		return EbMSDocument.builder()
				.contentId(contentId)
				.message(DOMUtils.read(message))
				.attachments(Collections.emptyList())
				.build();
	}

	private void parseEbMSMessage(EbMSContentHandler handler, String contentType, InputStream in) throws MimeException, IOException
	{
		val mimeConfig = MimeConfig.custom().setHeadlessParsing(contentType).build();
	  val parser = new MimeStreamParser(mimeConfig);
	  parser.setContentHandler(handler);
		parser.parse(in);
	}

	private EbMSDocument getEbMSMessage(InputStream message) throws ParserConfigurationException, SAXException, IOException
	{
		xsdValidator.validate(message);
		message.reset();
		return EbMSDocument.builder()
				.contentId(contentId)
				.message(DOMUtils.read(message))
				.attachments(Collections.emptyList())
				.build();
	}

	private EbMSDocument getEbMSMessage(List<EbMSAttachment> attachments) throws ParserConfigurationException, SAXException, IOException
	{
		if (attachments.size() > 0)
		{
			val message = attachments.remove(0);
			xsdValidator.validate(message.getInputStream());
			return EbMSDocument.builder()
					.contentId(contentId)
					.message(DOMUtils.read((message.getInputStream())))
					.attachments(attachments)
					.build();
		}
		return null;
	}

}
