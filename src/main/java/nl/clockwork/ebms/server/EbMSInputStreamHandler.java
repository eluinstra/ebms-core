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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

public abstract class EbMSInputStreamHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor messageProcessor;

	public EbMSInputStreamHandler(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

	public void handle(InputStream request) throws EbMSProcessorException
	{
	  try
		{
	  	String soapAction = getRequestHeader("SOAPAction");
	  	if (!Constants.EBMS_SOAP_ACTION.equals(soapAction))
	  	{
				if (logger.isDebugEnabled())
					logger.debug("<<<<\n" + IOUtils.toString(request));
				throw new EbMSProcessorException("Unable to process message! SOAPAction=" + soapAction);
	  	}

			EbMSMessageReader messageReader = new EbMSMessageReader(getRequestHeader("Content-ID"),getRequestHeader("Content-Type"));
			EbMSDocument in = messageReader.read(request);
			if (logger.isDebugEnabled())
				logger.debug("<<<<\n" + DOMUtils.toString(in.getMessage()));
			EbMSDocument out = messageProcessor.processRequest(in);
			if (out == null)
			{
				logger.debug(">>>> statusCode = " + 204 + "\n");
				writeResponseStatus(204);
			}
			else
			{
				if (logger.isDebugEnabled())
					logger.debug(">>>> statusCode = " + 200 + "\n" + DOMUtils.toString(out.getMessage()));
				writeResponseStatus(200);
				writeResponseHeader("Content-Type","text/xml");
				writeResponseHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
				OutputStream response = getOutputStream();
				DOMUtils.write(out.getMessage(),response);
			}
		}
		catch (Exception e)
		{
			try
			{
				Document soapFault = EbMSMessageUtils.createSOAPFault(e);
				if (logger.isDebugEnabled())
				{
					logger.debug(">>>> statusCode = " + 500 + "\n" + DOMUtils.toString(soapFault));
					logger.debug("",e);
				}
				writeResponseStatus(500);
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
	
	public abstract String getRequestHeader(String headerName);
	
	public abstract void writeResponseStatus(int statusCode);
	
	public abstract void writeResponseHeader(String name, String value);

	public abstract OutputStream getOutputStream() throws IOException;
	
}
