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
package nl.clockwork.ebms.client.apache;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

public class EbMSMessageWriter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	protected HttpPost httpPost;
	protected boolean chunkedStreamingMode = true;
	
	public EbMSMessageWriter(HttpPost httpPost)
	{
		this.httpPost = httpPost;
	}

	public EbMSMessageWriter(HttpPost httpPost, boolean chunkedStreamingMode)
	{
		this.httpPost = httpPost;
		this.chunkedStreamingMode = chunkedStreamingMode;
	}

	public void write(EbMSDocument document) throws IOException, TransformerException, UnsupportedEncodingException
	{
		if (document.getAttachments().size() > 0)
			writeMimeMessage(document);
		else
			writeMessage(document);
	}

	protected void writeMessage(EbMSDocument document) throws UnsupportedEncodingException, TransformerException
	{
		if (logger.isInfoEnabled())
			logger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		httpPost.setHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
		StringEntity entity = new StringEntity(DOMUtils.toString(document.getMessage(),"UTF-8"),"UTF-8");
		entity.setContentType("text/xml");
		entity.setChunked(chunkedStreamingMode);
		httpPost.setEntity(entity);
	}

	protected void writeMimeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		if (logger.isInfoEnabled())
			logger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		httpPost.setHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();
		entity.addPart(document.getContentId(),new StringBody(DOMUtils.toString(document.getMessage(),"UTF-8"),ContentType.create("text/xml")));
		for (EbMSAttachment attachment : document.getAttachments())
			if (attachment.getContentType().matches("^(text/.*|.*/xml)$"))
				writeTextAttachment(entity,attachment);
			else
				writeBinaryAttachment(entity,attachment);
		httpPost.setEntity(entity.build());
	}

	protected void writeTextAttachment(MultipartEntityBuilder entity, EbMSAttachment attachment) throws IOException
	{
		StringBody contentBody = new StringBody(IOUtils.toString(attachment.getInputStream()),ContentType.create(attachment.getContentType()));
		FormBodyPartBuilder formBodyPartBuilder = FormBodyPartBuilder.create(attachment.getName(),contentBody);
		formBodyPartBuilder.addField("Content-ID",attachment.getContentId());
		entity.addPart(formBodyPartBuilder.build());
	}

	protected void writeBinaryAttachment(MultipartEntityBuilder entity, EbMSAttachment attachment) throws IOException
	{
		InputStreamBody contentBody = new InputStreamBody(attachment.getInputStream(),ContentType.create(attachment.getContentType()),attachment.getName());
		FormBodyPartBuilder formBodyPartBuilder = FormBodyPartBuilder.create(attachment.getName(),contentBody);
		formBodyPartBuilder.addField("Content-ID",attachment.getContentId());
		entity.addPart(formBodyPartBuilder.build());
	}

}
