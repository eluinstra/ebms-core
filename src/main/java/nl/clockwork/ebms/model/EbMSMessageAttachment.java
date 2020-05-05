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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.io.IOUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

@Deprecated
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class EbMSMessageAttachment implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static final int BUFFERSIZE = 10000;
	@XmlElement(required=true)
	@NonNull
	EbMSMessageContext context;
	@XmlElement(name="attachment")
	List<DataHandler> attachments = new ArrayList<>();

	/*
	 * convert to MessageContent class for further processing
	 */
	public EbMSMessageContent toContent() throws EbMSProcessingException
	{
		return new EbMSMessageContent(context,attachments.stream().map(a -> toDataSource(a)).collect(Collectors.toList()));
	}

	private EbMSDataSource toDataSource(DataHandler a)
	{
		try
		{
			val contentType = new ContentType(a.getContentType());
			return new EbMSDataSource(contentType.getParameter("name"),contentType.getBaseType(),getByteArrayOutputStream(a).toByteArray());
		}
		catch (ParseException e)
		{
			return new EbMSDataSource(null,a.getContentType(),getByteArrayOutputStream(a).toByteArray());
		}
	}

	private ByteArrayOutputStream getByteArrayOutputStream(DataHandler a) throws EbMSProcessingException
	{
		try
		{
			val bos = new ByteArrayOutputStream();
			IOUtils.copy(a.getInputStream(),bos,BUFFERSIZE);
			return bos;
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException(e);
		}
	}	
}
