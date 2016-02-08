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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

public class EbMSMessageWriter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private HttpPost httpPost;
	private boolean chunkedStreamingMode = true;
	
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
		if (logger.isInfoEnabled())
			logger.info(">>>>\n" + DOMUtils.toString(document.getMessage()));
		if (document.getAttachments().size() > 0)
			writeMimeMessage(document);
		else
			writeMessage(document);
	}

	private void writeMessage(EbMSDocument document) throws UnsupportedEncodingException, TransformerException
	{
		httpPost.setHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
		StringEntity entity = new StringEntity(DOMUtils.toString(document.getMessage(),"UTF-8"),"UTF-8");
		entity.setContentType("text/xml");
		entity.setChunked(chunkedStreamingMode);
		httpPost.setEntity(entity);
	}

	private void writeMimeMessage(EbMSDocument document) throws IOException, TransformerException
	{
		httpPost.setHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
		MultipartEntityBuilder entity = MultipartEntityBuilder.create();
    entity.addPart("0",new StringBody(DOMUtils.toString(document.getMessage(),"UTF-8"),ContentType.create("text/xml; charset=UTF-8")));
		for (EbMSAttachment attachment : document.getAttachments())
	    entity.addPart(attachment.getContentId(),new InputStreamBody(attachment.getInputStream(),ContentType.create(attachment.getContentType()),attachment.getName()));
		httpPost.setEntity(entity.build());
	}

}
