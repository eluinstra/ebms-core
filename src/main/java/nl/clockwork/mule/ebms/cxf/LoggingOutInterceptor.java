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
package nl.clockwork.mule.ebms.cxf;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor.AttachmentOutEndingInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class LoggingOutInterceptor extends AbstractSoapInterceptor
{
  protected transient Log logger = LogFactory.getLog(getClass());

	public static final String OUTPUT_STREAM_HOLDER = LoggingOutInterceptor.class.getName() + ".outputstream";
  private int limit = 100 * 1024;

	public LoggingOutInterceptor()
	{
		super(Phase.PRE_STREAM);
		addBefore(AttachmentOutInterceptor.class.getName());
	}
	
	private String getEncoding(Message message)
	{
		Exchange ex = message.getExchange();
		String encoding = (String)message.get(Message.ENCODING);
		if (encoding == null && ex.getInMessage() != null)
		{
			encoding = (String) ex.getInMessage().get(Message.ENCODING);
			message.put(Message.ENCODING, encoding);
		}

		if (encoding == null)
		{
			encoding = "UTF-8";
			message.put(Message.ENCODING, encoding);
		}
		return encoding;
	}

	@Override
	public void handleMessage(SoapMessage message) throws Fault
	{
    	OutputStream originalOs = message.getContent(OutputStream.class);
			message.put(OUTPUT_STREAM_HOLDER,originalOs);
      CachedOutputStream cos = new CachedOutputStream();
      message.setContent(OutputStream.class,cos); 
            message.setContent(XMLStreamWriter.class, StaxUtils.createXMLStreamWriter(cos, getEncoding(message)));
	    message.getInterceptorChain().add(new LoggingOutEndingInterceptor());
	}

  public class LoggingOutEndingInterceptor extends AbstractSoapInterceptor
	{

  	public LoggingOutEndingInterceptor()
		{
      super(Phase.PRE_STREAM_ENDING);
  		addAfter(AttachmentOutEndingInterceptor.class.getName());
		}
  	
		@Override
		public void handleMessage(SoapMessage message) throws Fault
		{
			try
			{
				final LoggingMessage buffer = new LoggingMessage("Outbound Message\n---------------------------", LoggingMessage.nextId());
				buffer.getHeader().append(Message.CONTENT_TYPE).append(":").append(message.get(Message.CONTENT_TYPE));
				String encoding = (String)message.get(Message.ENCODING);
				if (encoding != null)
					buffer.getEncoding().append(encoding);
				Object headers = message.get(Message.PROTOCOL_HEADERS);
				if (headers != null)
					buffer.getHeader().append(headers);

				CachedOutputStream cos = (CachedOutputStream)message.getContent(OutputStream.class);
				OutputStream originalOs = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
				cos.writeCacheTo(buffer.getPayload(),limit);
				cos.writeCacheTo(originalOs);
				message.setContent(OutputStream.class,originalOs);
				
				if (logger.isInfoEnabled())
					logger.info(buffer.toString());
			}
			catch (IOException e)
			{
				new Fault(e);
			}
		}

	}

}
