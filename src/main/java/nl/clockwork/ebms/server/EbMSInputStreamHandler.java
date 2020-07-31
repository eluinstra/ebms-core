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

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageReader;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.HttpStatusCode;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.ValidationException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public abstract class EbMSInputStreamHandler
{
	private static final Logger messageLog = LoggerFactory.getLogger(Constants.MESSAGE_LOG);
  @NonNull
	EbMSMessageProcessor messageProcessor;

	public void handle(InputStream request) throws EbMSProcessorException
	{
	  try
		{
	  	val soapAction = getRequestHeader("SOAPAction");
	  	if (!Constants.EBMS_SOAP_ACTION.equals(soapAction))
	  	{
				if (messageLog.isInfoEnabled())
					messageLog.info("<<<<\n" + getRequestHeaders() + "\n" + IOUtils.toString(request,Charset.defaultCharset()));
				throw new ValidationException("Unable to process message! SOAPAction=" + soapAction);
	  	}
//	  	if (log.isDebugEnabled())
//	  		request = new LoggingInputStream(request);
	  	if (messageLog.isDebugEnabled())
	  	{
	  		request = new BufferedInputStream(request);
	  		request.mark(Integer.MAX_VALUE);
	  		messageLog.info("<<<<\n" + getRequestHeaders() + "\n" + IOUtils.toString(request,Charset.defaultCharset()));
	  		request.reset();
	  	}
			val messageReader = new EbMSMessageReader(getRequestHeader("Content-ID"),getRequestHeader("Content-Type"));
			val in = messageReader.read(request);
			if (messageLog.isInfoEnabled() && !messageLog.isDebugEnabled())
				messageLog.info("<<<<\n" + DOMUtils.toString(in.getMessage()));
			val out = messageProcessor.processRequest(in);
			if (out == null)
			{
				messageLog.info(">>>>\nstatusCode: " + HttpStatusCode.SC_NOCONTENT.getCode());
				writeResponseStatus(HttpStatusCode.SC_NOCONTENT.getCode());
			}
			else
			{
				if (messageLog.isInfoEnabled())
					messageLog.info(">>>>\nstatusCode: " + HttpStatusCode.SC_OK.getCode() + "\nContent-Type: text/xml\nSOAPAction: " + Constants.EBMS_SOAP_ACTION + "\n" + DOMUtils.toString(out.getMessage()));
				writeResponseStatus(HttpStatusCode.SC_OK.getCode());
				writeResponseHeader("Content-Type","text/xml");
				writeResponseHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
				val response = getOutputStream();
				DOMUtils.write(out.getMessage(),response);
			}
		}
		catch (Exception e)
		{
			try
			{
				val soapFault = EbMSMessageUtils.createSOAPFault(e);
				if (messageLog.isInfoEnabled())
					messageLog.info(">>>>\nstatusCode: " + HttpStatusCode.SC_INTERNAL_SERVER_ERROR.getCode() + "\nContent-Type: text/xml\n" + DOMUtils.toString(soapFault));
				log.info("",e);
				writeResponseStatus(HttpStatusCode.SC_INTERNAL_SERVER_ERROR.getCode());
				writeResponseHeader("Content-Type","text/xml");
				val response = getOutputStream();
				DOMUtils.write(soapFault,response);
			}
			catch (Exception e1)
			{
				throw new EbMSProcessorException(e1);
			}
		}
	}

	private String getRequestHeaders()
	{
		return getRequestHeaderNames().stream().flatMap(n -> getRequestHeaders(n).stream().map(h -> n + ": " + h)).collect(Collectors.joining("\n"));
	}
	
	public abstract List<String> getRequestHeaderNames();
	
	public abstract List<String> getRequestHeaders(String headerName);

	public abstract String getRequestHeader(String headerName);
	
	public abstract void writeResponseStatus(int statusCode);
	
	public abstract void writeResponseHeader(String name, String value);

	public abstract OutputStream getOutputStream() throws IOException;
	
}
