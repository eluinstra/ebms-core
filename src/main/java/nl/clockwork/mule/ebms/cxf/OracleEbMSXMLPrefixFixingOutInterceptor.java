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

import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

public class OracleEbMSXMLPrefixFixingOutInterceptor extends AbstractSoapInterceptor
{
	public static final String OUTPUT_STREAM_HOLDER = OracleEbMSXMLPrefixFixingOutInterceptor.class.getName() + ".outputstream";
	protected transient Log logger = LogFactory.getLog(getClass());

	public OracleEbMSXMLPrefixFixingOutInterceptor()
	{
		super(Phase.WRITE);
		addBefore(SoapOutInterceptor.class.getName());
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
    try
		{
    	OutputStream originalOs = message.getContent(OutputStream.class);
			message.put(OUTPUT_STREAM_HOLDER,originalOs);
      CachedOutputStream cos = new CachedOutputStream();
      message.setContent(OutputStream.class,cos); 
			message.setContent(XMLStreamWriter.class,StaxOutInterceptor.getXMLOutputFactory(message).createXMLStreamWriter(cos,getEncoding(message)));
	    message.getInterceptorChain().add(new OracleEbMSXMLPrefixFixingOutEndingInterceptor()); 
		}
		catch (Exception e)
		{
			throw new Fault(e);
		}
	}
	
	public class OracleEbMSXMLPrefixFixingOutEndingInterceptor extends AbstractSoapInterceptor
	{
		public OracleEbMSXMLPrefixFixingOutEndingInterceptor()
		{
			super(OracleEbMSXMLPrefixFixingOutEndingInterceptor.class.getName(),Phase.WRITE_ENDING);
			addAfter(SoapOutInterceptor.SoapOutEndingInterceptor.class.getName());
		}
	
		@Override
		public void handleMessage(final SoapMessage message) throws Fault
		{
			try
			{
				CachedOutputStream os = (CachedOutputStream)message.getContent(OutputStream.class);
				StringBuilder sb = new StringBuilder();
				os.writeCacheTo(sb);
				
				OutputStream originalOs = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
				Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(this.getClass().getResourceAsStream("/nl/clockwork/mule/ebms/xsl/EbMSNullTransformation.xml")));
				transformer.transform(new StreamSource(new StringReader(sb.toString())),new StreamResult(originalOs));

				message.setContent(OutputStream.class,originalOs);
			}
			catch (Exception e)
			{
				throw new Fault(e);
			}
		}
	
	}

}
