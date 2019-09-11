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
package nl.clockwork.ebms.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlElement;

import nl.clockwork.ebms.processor.EbMSProcessingException;

public class EbMSMessageAttachment implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final int BUFFERSIZE = 10000;
	private EbMSMessageContext context;
	private transient List<DataHandler> attachments;
	private EbMSMessageContent msgContentCache = null;
	
	@XmlElement(required=true)
	public EbMSMessageContext getContext()
	{
		return context;
	}
	
	public void setContext(EbMSMessageContext context)
	{
		this.context = context;
	}
	
	@XmlElement(name="attachment",required=true)
	public List<DataHandler> getAttachments()
	{
		return attachments;
	}
	
	public void setAttachments(List<DataHandler> attachments)
	{
		this.attachments = attachments;
	}
	
	/*
	 * convert to MessageContent class for further processing
	 */
	public EbMSMessageContent toContent() throws EbMSProcessingException
	{
		if (msgContentCache == null)
		{
			msgContentCache = new EbMSMessageContent(context);
			attachments.forEach(a -> msgContentCache.getDataSources().add(new EbMSDataSource(a.getName(),a.getContentType(),getByteArrayOutputStream(a).toByteArray())));
		}
		
		return msgContentCache;
	}

	private ByteArrayOutputStream getByteArrayOutputStream(DataHandler a) throws EbMSProcessingException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			InputStream input = a.getInputStream();
			byte[] b = new byte[BUFFERSIZE];
			int bytesRead = 0;
			while ((bytesRead = input.read(b)) != -1)
			{
				bos.write(b, 0, bytesRead);
			}
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
		return bos;
	}	
}
