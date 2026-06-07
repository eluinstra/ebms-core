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
package nl.clockwork.ebms.server;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.xml.bind.JAXBException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.common.Constants;
import nl.clockwork.ebms.common.message.EbMSMessageReader;
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSDocument;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.validation.ValidationException;
import nl.clockwork.ebms.server.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.server.processor.EbMSProcessingException;
import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class EbMSInputStreamHandler
{
	public static final long DEFAULT_MAX_REQUEST_BYTES = 5L * 1024L * 1024L;
	public static final int DEFAULT_MAX_LOGGED_PAYLOAD_CHARS = 8192;

	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	EbMSMessageProcessor messageProcessor;
	long maxRequestBytes;
	int maxLoggedPayloadChars;

	protected EbMSInputStreamHandler(@NonNull EbMSMessageProcessor messageProcessor)
	{
		this(messageProcessor, DEFAULT_MAX_REQUEST_BYTES, DEFAULT_MAX_LOGGED_PAYLOAD_CHARS);
	}

	protected EbMSInputStreamHandler(@NonNull EbMSMessageProcessor messageProcessor, long maxRequestBytes, int maxLoggedPayloadChars)
	{
		this.messageProcessor = messageProcessor;
		this.maxRequestBytes = maxRequestBytes;
		this.maxLoggedPayloadChars = maxLoggedPayloadChars;
	}

	public void handle(InputStream request)
	{
		try
		{
			val responseDocument = handleRequest(request);
			returnResponse(responseDocument);
		}
		catch (ValidationException e)
		{
			log.error("", e);
			handleValidationException("Client", e.getMessage());
		}
		catch (IOException | MimeException | ParserConfigurationException | SAXException | TransformerException | RuntimeException e)
		{
			log.error("", e);
			handleException();
		}
	}

	public abstract List<String> getRequestHeaderNames();

	public abstract List<String> getRequestHeaders(String headerName);

	public abstract String getRequestHeader(String headerName);

	public abstract String getRequestMethod();

	public abstract long getRequestContentLength();

	public abstract void writeResponseStatus(int statusCode);

	public abstract void writeResponseHeader(String name, String value);

	public abstract OutputStream getOutputStream() throws IOException;

	private EbMSDocument handleRequest(InputStream request) throws IOException, MimeException, ParserConfigurationException, SAXException, TransformerException
	{
		validateRequest();
		validateRequestSize();
		validateSoapAction(request);
		if (messageLog.isDebugEnabled())
			request = getRequestLogger(request);
		val messageReader = new EbMSMessageReader(getRequestHeader("Content-ID"), getRequestHeader("Content-Type"));
		val requestDocument = messageReader.read(request);
		if (messageLog.isInfoEnabled() && !messageLog.isDebugEnabled())
			messageLog.info("<<<<\n{}", DOMUtils.toString(requestDocument.getMessage()));
		return messageProcessor.processRequest(requestDocument);
	}

	private void validateRequest()
	{
		if (!"POST".equals(getRequestMethod()))
			throw new EbMSProcessingException("Not allowed RequestMethod=" + getRequestMethod());
	}

	private void validateRequestSize()
	{
		val contentLength = getRequestContentLength();
		if (contentLength > maxRequestBytes)
			throw new ValidationException("Unable to process message! Request too large");
	}

	private void validateSoapAction(InputStream request) throws IOException
	{
		val soapAction = getRequestHeader("SOAPAction");
		if (!Constants.EBMS_SOAP_ACTION.equals(soapAction))
		{
			if (messageLog.isInfoEnabled())
				messageLog.info("<<<<\n{}\n{}", getRequestHeaders(), getPayloadPreview(request));
			throw new ValidationException("Unable to process message! SOAPAction=" + soapAction);
		}
	}

	private InputStream getRequestLogger(InputStream request) throws IOException
	{
		val result = new BufferedInputStream(request);
		result.mark(maxLoggedPayloadChars + 1);
		messageLog.info("<<<<\n{}\n{}", getRequestHeaders(), getPayloadPreview(result));
		result.reset();
		return result;
	}

	private String getRequestHeaders()
	{
		return getRequestHeaderNames().stream()
				.flatMap(n -> getRequestHeaders(n).stream().map(h -> sanitizeForLog(n) + "=" + sanitizeForLog(h)))
				.collect(Collectors.joining("\n"));
	}

	private String getPayloadPreview(InputStream request) throws IOException
	{
		val payload = request.readNBytes(maxLoggedPayloadChars + 1);
		val truncated = payload.length > maxLoggedPayloadChars;
		val length = Math.min(payload.length, maxLoggedPayloadChars);
		val preview = new String(payload, 0, length, StandardCharsets.UTF_8);
		return truncated ? preview + "\n... (truncated)" : preview;
	}

	private String sanitizeForLog(String value)
	{
		if (value == null)
			return "";
		return value.replaceAll("[\\r\\n\\t]", "_");
	}

	private void returnResponse(final nl.clockwork.ebms.common.model.EbMSDocument responseDocument) throws TransformerException, IOException
	{
		if (responseDocument == null)
		{
			messageLog.info(">>>>\nStatusCode={}", HttpServletResponse.SC_NO_CONTENT);
			writeResponseStatus(HttpServletResponse.SC_NO_CONTENT);
		}
		else
		{
			if (messageLog.isInfoEnabled())
				messageLog.info(
						">>>>\nStatusCode={}\nContent-Type=text/xml\nSOAPAction={}\n{}",
						HttpServletResponse.SC_OK,
						Constants.EBMS_SOAP_ACTION,
						DOMUtils.toString(responseDocument.getMessage()));
			writeResponseStatus(HttpServletResponse.SC_OK);
			writeResponseHeader("Content-Type", "text/xml");
			writeResponseHeader("SOAPAction", Constants.EBMS_SOAP_ACTION);
			val response = getOutputStream();
			DOMUtils.write(responseDocument.getMessage(), response);
		}
	}

	private void handleValidationException(String faultCode, String faultString)
	{
		try
		{
			val soapFault = EbMSMessageUtils.createSOAPFault(faultCode, faultString);
			if (messageLog.isInfoEnabled())
				messageLog.info(">>>>\nStatusCode={}\nContent-Type=text/xml\n{}", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, DOMUtils.toString(soapFault));
			writeResponseStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeResponseHeader("Content-Type", "text/xml");
			val response = getOutputStream();
			DOMUtils.write(soapFault, response);
		}
		catch (IOException | ParserConfigurationException | JAXBException | SAXException | TransformerException | RuntimeException e)
		{
			log.error("", e);
			throw new IllegalStateException("An unexpected error occurred!");
		}
	}

	private void handleException()
	{
		messageLog.info(">>>>\nStatusCode={}", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		writeResponseStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	}
}
