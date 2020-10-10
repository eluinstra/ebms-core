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
package nl.clockwork.ebms.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.ValidationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.MimeException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class EbMSInputStreamHandler
{
  protected transient Log logger = LogFactory.getLog(nl.clockwork.ebms.server.EbMSInputStreamHandler.class);
  protected transient Log messageLogger = LogFactory.getLog(Constants.MESSAGE_LOG);
	private EbMSMessageProcessor messageProcessor;

	public EbMSInputStreamHandler(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

	public void handle(InputStream request) throws EbMSProcessorException
	{
	  try
		{
	  	EbMSDocument responseDocument = handleRequest(request);
			returnResponse(responseDocument);
		}
	  catch (ValidationException e)
	  {
			logger.error("",e);
			handleException("Client",e.getMessage());
	  }
		catch (Exception e)
		{
			logger.error("",e);
			writeResponseStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
		if (messageLogger.isDebugEnabled())
			request = getRequestLogger(request);
		EbMSMessageReader messageReader = new EbMSMessageReader(getRequestHeader("Content-ID"),getRequestHeader("Content-Type"));
		EbMSDocument requestDocument = messageReader.read(request);
		if (messageLogger.isInfoEnabled() && !messageLogger.isDebugEnabled())
			messageLogger.info("<<<<\n" + DOMUtils.toString(requestDocument.getMessage()));
		EbMSDocument responseDocument = messageProcessor.processRequest(requestDocument);
		return responseDocument;
	}

	private void validateRequest()
	{
		if (!"POST".equals(getRequestMethod()))
			throw new EbMSProcessingException("Not allowed RequestMethod=" + getRequestMethod());
	}

	private void validateSoapAction(InputStream request) throws IOException
	{
		String soapAction = getRequestHeader("SOAPAction");
		if (!Constants.EBMS_SOAP_ACTION.equals(soapAction))
		{
			if (messageLogger.isInfoEnabled())
				messageLogger.info("<<<<\n" + getRequestHeaders() + "\n" + IOUtils.toString(request,Charset.defaultCharset()));
			throw new EbMSProcessorException("Unable to process message! SOAPAction=" + soapAction);
		}
	}

	private InputStream getRequestLogger(InputStream request) throws IOException
	{
		request = new BufferedInputStream(request);
		request.mark(Integer.MAX_VALUE);
		messageLogger.info("<<<<\n" + getRequestHeaders() + "\n" + IOUtils.toString(request,Charset.defaultCharset()));
		request.reset();
		return request;
	}

	private String getRequestHeaders()
	{
		return getRequestHeaderNames().stream().flatMap(n -> getRequestHeaders(n).stream().map(h -> n + ": " + h)).collect(Collectors.joining("\n"));
	}
	
	private void returnResponse(EbMSDocument responseDocument) throws TransformerException, IOException
	{
		if (responseDocument == null)
		{
			messageLogger.info(">>>>\nstatusCode: " + Constants.SC_NOCONTENT);
			writeResponseStatus(Constants.SC_NOCONTENT);
		}
		else
		{
			if (messageLogger.isInfoEnabled())
				messageLogger.info(">>>>\nstatusCode: " + Constants.SC_OK + "\nContent-Type: text/xml\nSOAPAction: " + Constants.EBMS_SOAP_ACTION + "\n" + DOMUtils.toString(responseDocument.getMessage()));
			writeResponseStatus(Constants.SC_OK);
			writeResponseHeader("Content-Type","text/xml");
			writeResponseHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
			OutputStream response = getOutputStream();
			DOMUtils.write(responseDocument.getMessage(),response);
		}
	}

	private void handleException(String faultCode, String faultString)
	{
		try
		{
			Document soapFault = EbMSMessageUtils.createSOAPFault(faultCode,faultString);
			if (messageLogger.isInfoEnabled())
				messageLogger.info(">>>>\nstatusCode: " + Constants.SC_INTERNAL_SERVER_ERROR + "\nContent-Type: text/xml\n" + DOMUtils.toString(soapFault));
			writeResponseStatus(Constants.SC_INTERNAL_SERVER_ERROR);
			writeResponseHeader("Content-Type","text/xml");
			OutputStream response = getOutputStream();
			DOMUtils.write(soapFault,response);
		}
		catch (Exception e1)
		{
			throw new EbMSProcessorException(e1);
		}
	}

}
