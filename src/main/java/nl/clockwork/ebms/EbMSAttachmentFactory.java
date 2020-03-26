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

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.io.CachedOutputStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import nl.clockwork.ebms.model.CachedEbMSAttachment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.PlainEbMSAttachment;

public class EbMSAttachmentFactory implements InitializingBean
{
	private static int attachmentMemoryTreshold;
	@SuppressWarnings("unused")
	private EbMSAttachmentFactory attachmentFactory;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		CachedOutputStream.setDefaultThreshold(attachmentMemoryTreshold);
		attachmentFactory = this;
	}

	public static EbMSAttachment createCachedEbMSAttachment(String contentId, DataSource ds)
	{
		return new PlainEbMSAttachment(contentId,ds);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentType, byte[] content)
	{
		return createEbMSAttachment(filename,null,contentType,content);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentId, String contentType, byte[] content)
	{
		ByteArrayDataSource result = new ByteArrayDataSource(content,contentType);
		if (!StringUtils.isEmpty(filename))
			result.setName(filename);
		return createCachedEbMSAttachment(contentId,result);
	}

	public static EbMSAttachment createEbMSAttachment(String filename, String contentId, String contentType, InputStream content) throws IOException
	{
		ByteArrayDataSource result = new ByteArrayDataSource(content,contentType);
		if (!StringUtils.isEmpty(filename))
			result.setName(filename);
		return createCachedEbMSAttachment(contentId,result);
	}

	public static EbMSAttachment createCachedEbMSAttachment(String filename, String contentId, String contentType, InputStream content) throws IOException
	{
		return createCachedEbMSAttachment(filename,contentId,contentType,content,0);
	}

	public static EbMSAttachment createCachedEbMSAttachment(String filename, String contentId, String contentType, InputStream content, long length) throws IOException
	{
		CachedOutputStream cos = length >= attachmentMemoryTreshold ? new CachedOutputStream(0) : new CachedOutputStream();
		CachedOutputStream.copyStream(content,cos,4096);
		cos.lockOutputStream();
		return new CachedEbMSAttachment(filename,contentId,contentType,cos);
	}

	public static void setAttachmentMemoryTreshold(int treshold)
	{
		attachmentMemoryTreshold = treshold;
	}
}
