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
	private String soapAction;
	private String contentType;

	public EbMSInputStreamHandlerImpl(String soapAction, String contentType)
	{
		this.soapAction = soapAction;
		this.contentType = contentType;
	}

	@Override
	public void handle(InputStream request) throws EbMSProcessorException
	{
	  try
		{
	  	if (Constants.EBMS_SOAP_ACTION.equals(soapAction))
	  	{
	  		EbMSMessageReader messageReader = new EbMSMessageReaderImpl(contentType);
				EbMSDocument in = messageReader.read(request);
				EbMSDocument out = messageProcessor.process(in);
				if (out == null)
					writeMessage(204);
				else
				{
					Map<String,String> headers = new HashMap<String,String>();
					headers.put("Content-Type","text/xml");
					headers.put("SOAPAction",Constants.EBMS_SOAP_ACTION);
					OutputStream response = writeMessage(headers,200);
					DOMUtils.write(out.getMessage(),response);
				}
	  	}
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	  
	}
	
	public abstract void writeMessage(int statusCode);

	public abstract OutputStream writeMessage(Map<String,String> headers, int statusCode);

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

}
