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
package nl.clockwork.ebms.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSSoapServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
  protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public void service(final ServletRequest request, ServletResponse response) throws ServletException, IOException
	{
		//log(request);
		try
		{
			//SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			//soapConnectionFactory.createConnection();
			//SOAPFactory.newInstance().createElement(domElement);
			MimeHeaders mimeHeaders = getMimeHeader((HttpServletRequest)request);
			SOAPMessage message = MessageFactory.newInstance().createMessage(mimeHeaders,request.getInputStream());
			message.getSOAPPart();
			message.getSOAPHeader();
			message.getSOAPBody();
			message.getAttachments();
		}
		catch (UnsupportedOperationException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (SOAPException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}

	
	private MimeHeaders getMimeHeader(HttpServletRequest request)
	{
		MimeHeaders result = new MimeHeaders();
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements())
		{
			String headerName = headerNames.nextElement();
			result.addHeader(headerName,request.getHeader(headerName));
		}
		return result;
	}


	private void log(HttpServletRequest request) throws IOException
	{
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements())
		{
			String headerName = headerNames.nextElement();
			logger.info(headerName + ": " + request.getHeader(headerName));
//			Enumeration<String> headerValues = request.getHeaders(headerName);
//			while (headerValues.hasMoreElements())
//				logger.info(headerName + ": " + headerValues.nextElement());
		}
		BufferedReader reader = request.getReader();
		StringBuffer s = new StringBuffer();
		String in = null;
		while ((in = reader.readLine()) != null)
			s.append(in);
		logger.info(s);
	}

	
}
