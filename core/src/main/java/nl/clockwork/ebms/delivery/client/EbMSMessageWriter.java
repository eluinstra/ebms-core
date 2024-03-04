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
package nl.clockwork.ebms.delivery.client;

import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.util.DOMUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class EbMSMessageWriter
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);

	public Builder write(Builder request, EbMSDocument document) throws TransformerException
	{
		return document.getAttachments().isEmpty() ? writeMessage(request, document) : writeMimeMessage(request, document);
	}

	protected Builder writeMessage(Builder request, EbMSDocument document) throws TransformerException
	{
		val message = DOMUtils.toString(document.getMessage(), "UTF-8");
		if (messageLog.isInfoEnabled())
			messageLog.info(">>>>\n{}", message);
		return request.setHeader("Content-Type", "text/xml; charset=UTF-8")
				.setHeader("SOAPAction", Constants.EBMS_SOAP_ACTION)
				.POST(BodyPublishers.ofString(message, StandardCharsets.UTF_8));
	}

	protected Builder writeMimeMessage(Builder request, EbMSDocument document) throws TransformerException
	{
		val message = DOMUtils.toString(document.getMessage(), "UTF-8");
		if (messageLog.isInfoEnabled())
			messageLog.info(">>>>\n{}", message);

		val publisher = new MultipartBodyPublisher(document.getContentId()).addXml(document.getContentId(), message);
		document.getAttachments().stream().forEach(publisher::addAttachment);
		return request.setHeader("Content-Type", publisher.contentType()).setHeader("SOAPAction", Constants.EBMS_SOAP_ACTION).POST(publisher);
	}
}
