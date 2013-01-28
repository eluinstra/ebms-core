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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbMSServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
  protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public void service(final ServletRequest request, ServletResponse response) throws ServletException, IOException
	{
		//log(request);
		try
		{
			Session s = Session.getDefaultInstance(new Properties());
			InputStream inputStream = new SequenceInputStream(new ByteArrayInputStream(("MIME-Version: 1.0\nContent-Type: " + request.getContentType() + "\n").getBytes()),request.getInputStream());
			MimeMessage message = new MimeMessage(s,inputStream);

			//MimeMultipart m = new MimeMultipart(new ServletMultipartDataSource(request));

			logger.info("---");
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
		}
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
