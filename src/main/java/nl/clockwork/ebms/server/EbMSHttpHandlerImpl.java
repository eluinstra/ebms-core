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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSHttpHandlerImpl implements EbMSHttpHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSMessageProcessor messageProcessor;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) throws EbMSProcessorException
	{
	  try
		{
	  	if (Constants.EBMS_SOAP_ACTION.equals(getHeader(request,"SOAPAction")))
	  	{
	  		EbMSMessageReader messageReader = new EbMSMessageReaderImpl(request.getContentType());
				EbMSDocument in = messageReader.read(request.getInputStream());
				//request.getInputStream().close();
				if (logger.isDebugEnabled())
					logger.debug("IN:\n" + DOMUtils.toString(in.getMessage()));
				EbMSDocument out = messageProcessor.processRequest(in);
				if (out == null)
				{
					logger.debug("OUT:\n");
					response.setStatus(204);
				}
				else
				{
					if (logger.isDebugEnabled())
						logger.debug("OUT:\n" + DOMUtils.toString(out.getMessage()));
					response.setStatus(200);
					response.setHeader("Content-Type","text/xml");
					response.setHeader("SOAPAction",Constants.EBMS_SOAP_ACTION);
					DOMUtils.write(out.getMessage(),response.getOutputStream());
					//response.getOutputStream().close();
				}
	  	}
	  	else
	  	{
	  		if (logger.isDebugEnabled())
	  		{
	  			logger.debug("Request ignored! SOAPAction: " + (StringUtils.isEmpty(getHeader(request,"SOAPAction"))? "<empty>" : getHeader(request,"SOAPAction")));
	  			logger.debug("IN:\n" + IOUtils.toString(request.getInputStream()));
	  		}
	  	}
		}
		catch (IOException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (TransformerException e)
		{
			throw new EbMSProcessorException(e);
		}
	  
	}
	
	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

	private String getHeader(HttpServletRequest request, String headerName)
	{
		String result = request.getHeader(headerName);
		if (result == null)
			while (request.getHeaderNames().hasMoreElements())
			{
				String key = (String)request.getHeaderNames().nextElement();
				if (headerName.equalsIgnoreCase(key))
				{
					result = request.getHeader(key);
					break;
				}
			}
		return result;
	}

}
