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
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.model.CachedEbMSAttachment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.PlainEbMSAttachment;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.io.CachedOutputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EbMSAttachmentFactory
{
	public static void init(String attachmentOutputDirectory, int attachmentMemoryTreshold, String attachmentCipherTransformation)
	{
		if (StringUtils.isNotEmpty(attachmentOutputDirectory))
			System.setProperty("org.apache.cxf.io.CachedOutputStream.OutputDirectory", attachmentOutputDirectory);
		CachedOutputStream.setDefaultThreshold(attachmentMemoryTreshold);
		if (StringUtils.isNotEmpty(attachmentCipherTransformation))
			CachedOutputStream.setDefaultCipherTransformation(attachmentCipherTransformation);
	}

	public static EbMSAttachment createEbMSAttachment(String contentId, DataSource ds)
	{
		return new PlainEbMSAttachment(contentId, ds);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentType, byte[] content)
	{
		return createEbMSAttachment(filename, null, contentType, content);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentId, String contentType, byte[] content)
	{
		val result = new ByteArrayDataSource(content, contentType);
		if (!StringUtils.isEmpty(filename))
			result.setName(filename);
		return createEbMSAttachment(contentId, result);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentId, String contentType, InputStream content) throws IOException
	{
		val result = new ByteArrayDataSource(content, contentType);
		if (!StringUtils.isEmpty(filename))
			result.setName(filename);
		return createEbMSAttachment(contentId, result);
	}

	public static EbMSAttachment createCachedEbMSAttachment(String contentId, DataHandler dataHandler) throws IOException
	{
		return createCachedEbMSAttachment(dataHandler.getName(), contentId, dataHandler.getContentType(), dataHandler.getInputStream());
	}

	public static EbMSAttachment createCachedEbMSAttachment(String filename, String contentId, String contentType, CachedOutputStream content) throws IOException
	{
		return new CachedEbMSAttachment(filename, contentId, contentType, content);
	}

	public static EbMSAttachment createCachedEbMSAttachment(String filename, String contentId, String contentType, InputStream content) throws IOException
	{
		val cos = new CachedOutputStream();
		CachedOutputStream.copyStream(content, cos, IOUtils.DEFAULT_BUFFER_SIZE);
		cos.lockOutputStream();
		return createCachedEbMSAttachment(filename, contentId, contentType, cos);
	}
}
