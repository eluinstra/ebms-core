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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;

import lombok.AccessLevel;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.model.EbMSAttachment;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class EbMSContentHandler implements ContentHandler
{
	Map<String,String> headers = new HashMap<>();
	List<EbMSAttachment> attachments = new ArrayList<>();

	@Override
	public void startMessage() throws MimeException
	{
	}

	@Override
	public void endMessage() throws MimeException
	{
	}

	@Override
	public void startBodyPart() throws MimeException
	{
	}

	@Override
	public void endBodyPart() throws MimeException
	{
	}

	@Override
	public void startHeader() throws MimeException
	{
	}

	@Override
	public void field(Field rawField) throws MimeException
	{
		headers.put(rawField.getName(),rawField.getBody());
	}

	@Override
	public void endHeader() throws MimeException
	{
	}

	@Override
	public void preamble(InputStream is) throws MimeException, IOException
	{
	}

	@Override
	public void epilogue(InputStream is) throws MimeException, IOException
	{
	}

	@Override
	public void startMultipart(BodyDescriptor bd) throws MimeException
	{
	}

	@Override
	public void endMultipart() throws MimeException
	{
	}

	@Override
	public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException
	{
		val filename = getFilename();
		val contentId = getContentId();
		val contentType = getContentType();
		val content = applyTransferEncoding(is);
		if (attachments.size() == 0)
			attachments.add(EbMSAttachmentFactory.createEbMSAttachment(filename,contentId,contentType,content));
		else
			attachments.add(EbMSAttachmentFactory.createCachedEbMSAttachment(filename,contentId,contentType,content));
		headers.clear();
	}

	@Override
	public void raw(InputStream is) throws MimeException, IOException
	{
	}

	public List<EbMSAttachment> getAttachments()
	{
		return attachments;
	}

	private String getHeader(String headerName)
	{
		var result = headers.get(headerName);
		if (result == null)
			result = headers.entrySet().stream().filter(e -> headerName.equalsIgnoreCase(e.getKey())).findFirst().map(e -> e.getValue()).orElse(null);
		return result;
	}

	private InputStream applyTransferEncoding(InputStream result)
	{
		val encoding = getHeader("Content-Transfer-Encoding");
		if (encoding != null && encoding.equalsIgnoreCase("base64"))
			result = new Base64InputStream(result);
		return result;
	}

	private String getFilename()
	{
		var result = getHeader("Content-Disposition");
		if (result != null && result.startsWith("attachment"))
			result = result.replaceAll("^attachment;\\s+filename=\"?([^\"]*)\"?$","$1");
		return result;
	}

	private String getContentId()
	{
		var result = getHeader("Content-ID");
		if (result != null)
			result = result.replaceAll("^<(.*)>$|^(.*)$","$1$2");
		return result;
	}

	private String getContentType()
	{
		return getHeader("Content-Type");
	}

}
