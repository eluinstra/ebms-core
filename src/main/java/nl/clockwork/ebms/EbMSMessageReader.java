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
package nl.clockwork.ebms;


import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.util.DOMUtils;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.xml.sax.SAXException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageReader
{
	String contentId;
	@NonNull
	String contentType;

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

	private void parseEbMSMessage(EbMSContentHandler handler, String contentType, InputStream in) throws MimeException, IOException
	{
		val mimeConfig = MimeConfig.custom().setHeadlessParsing(contentType).build();
		val parser = new MimeStreamParser(mimeConfig);
		parser.setContentHandler(handler);
		parser.parse(in);
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws ParserConfigurationException, SAXException, IOException
	{
		return EbMSDocument.builder().contentId(contentId).message(DOMUtils.read(in)).attachments(Collections.emptyList()).build();
	}

	private EbMSDocument getEbMSMessage(List<EbMSAttachment> attachments) throws ParserConfigurationException, SAXException, IOException
	{
		if (!attachments.isEmpty())
		{
			val message = attachments.remove(0);
			return EbMSDocument.builder().contentId(contentId).message(DOMUtils.read((message.getInputStream()))).attachments(attachments).build();
		}
		return null;
	}

}
