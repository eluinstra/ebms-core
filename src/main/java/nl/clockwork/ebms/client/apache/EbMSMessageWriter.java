/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.client.apache;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.FormBodyPart;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;

public class EbMSMessageWriter
{
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
    MultipartEntity entity = new MultipartEntity();
    FormBodyPart part = new FormBodyPart("0",new StringBody(DOMUtils.toString(document.getMessage(),"UTF-8"),"text/xml; charset=UTF-8",Charset.forName("UTF-8")));
    part.addField("Content-ID","<0>");
    entity.addPart(part);
		for (EbMSAttachment attachment : document.getAttachments())
		{
			part = new FormBodyPart(attachment.getContentId(),new InputStreamBody(attachment.getDataSource().getInputStream(),attachment.getContentType(),attachment.getName()));
	    part.addField("Content-ID","<" + attachment.getContentId() + ">");
	    entity.addPart(part);
		}
		httpPost.setEntity(entity);
	}

}
