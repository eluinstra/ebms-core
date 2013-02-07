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
package nl.clockwork.ebms.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class EbMSInputStreamHandlerImpl implements EbMSInputStreamHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor messageProcessor;
	private Map<String,String> headers = new HashMap<String,String>();

	public EbMSInputStreamHandlerImpl(EbMSMessageProcessor messageProcessor, Map<String,String> headers)
	{
		this.messageProcessor = messageProcessor;
		this.headers  = headers;
	}

	@Override
	public void handle(InputStream request) throws EbMSProcessorException
	{
	  try
		{
	  	if (Constants.EBMS_SOAP_ACTION.equals(getHeader("SOAPAction")))
	  	{
	  		EbMSMessageReader messageReader = new EbMSMessageReaderImpl(getHeader("Content-Type"));
				EbMSDocument in = messageReader.read(request);
				logger.debug("IN:\n" + DOMUtils.toString(in.getMessage()));
				EbMSDocument out = messageProcessor.process(in);
				if (out == null)
				{
					logger.debug("StatusCode: 204");
					writeResponseStatus(204);
				}
				else
				{
					logger.debug("StatusCode: 200");
					logger.debug("Content-Type: text/xml");
					logger.debug("OUT:\n" + DOMUtils.toString(out.getMessage()));
					writeResponseStatus(200);
					writeResponseHeader("Content-Type","text/xml");
					writeResponseHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
					OutputStream response = getOutputStream();
					DOMUtils.write(out.getMessage(),response);
				}
	  	}
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	  
	}
	
	private String getHeader(String headerName)
	{
		for (String key : headers.keySet())
			if (headerName.toLowerCase().equals(key.toLowerCase()))
				return headers.get(key);
		return null;
	}

	public abstract void writeResponseStatus(int statusCode);
	
	public abstract void writeResponseHeader(String name, String value);

	public abstract OutputStream getOutputStream() throws IOException;
	
	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

}
