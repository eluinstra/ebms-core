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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageReader;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.ValidationException;
import org.apache.commons.io.IOUtils;
import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public abstract class EbMSInputStreamHandler
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
	@NonNull
	EbMSMessageProcessor messageProcessor;

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
		catch (Exception e)
		{
			log.error("", e);
			handleException();
		}
	}

	public abstract List<String> getRequestHeaderNames();

	public abstract List<String> getRequestHeaders(String headerName);

	public abstract String getRequestHeader(String headerName);

	public abstract String getRequestMethod();

	public abstract void writeResponseStatus(int statusCode);

	public abstract void writeResponseHeader(String name, String value);

	public abstract OutputStream getOutputStream() throws IOException;

	private EbMSDocument handleRequest(InputStream request) throws IOException, MimeException, ParserConfigurationException, SAXException, TransformerException
	{
		validateRequest();
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

	private void validateSoapAction(InputStream request) throws IOException
	{
		val soapAction = getRequestHeader("SOAPAction");
		if (!Constants.EBMS_SOAP_ACTION.equals(soapAction))
		{
			if (messageLog.isInfoEnabled())
				messageLog.info("<<<<\n{}\n{}", getRequestHeaders(), IOUtils.toString(request, Charset.defaultCharset()));
			throw new ValidationException("Unable to process message! SOAPAction=" + soapAction);
		}
	}

	private InputStream getRequestLogger(InputStream request) throws IOException
	{
		val result = new BufferedInputStream(request);
		result.mark(Integer.MAX_VALUE);
		messageLog.info("<<<<\n{}\n{}", getRequestHeaders(), IOUtils.toString(result, Charset.defaultCharset()));
		result.reset();
		return result;
	}

	private String getRequestHeaders()
	{
		return getRequestHeaderNames().stream().flatMap(n -> getRequestHeaders(n).stream().map(h -> n + "=" + h)).collect(Collectors.joining("\n"));
	}

	private void returnResponse(final nl.clockwork.ebms.model.EbMSDocument responseDocument) throws TransformerException, IOException
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
		catch (Exception e)
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
