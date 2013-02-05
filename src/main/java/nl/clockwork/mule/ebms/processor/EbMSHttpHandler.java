package nl.clockwork.mule.ebms.processor;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.server.EbMSMessageReader;
import nl.clockwork.ebms.server.EbMSMessageReaderImpl;

import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

public class EbMSHttpHandler implements Callable
{
	private EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		
		MuleMessage message = eventContext.getMessage();
  	ContentLengthInputStream request = (ContentLengthInputStream)message.getPayload();
  	HttpResponse response = new HttpResponse();
		if (Constants.EBMS_SOAP_ACTION.equals(getHeader(message,"SOAPAction")))
  	{
  		EbMSMessageReader messageReader = new EbMSMessageReaderImpl(getHeader(message,"Content-Type"));
			EbMSDocument in = messageReader.read(request);
			final EbMSDocument out = ebMSMessageProcessor.process(in);
			if (out == null)
				response.setStatusLine(HttpVersion.parse(HttpConstants.HTTP11),204);
			else
			{
				response.setStatusLine(HttpVersion.parse(HttpConstants.HTTP11),200);
				response.setHeader(new Header("Content-Type","text/xml"));
				response.setHeader(new Header("SOAPAction",Constants.EBMS_SOAP_ACTION));
				response.setBody(
					new OutputHandler()
					{
						@Override
						public void write(MuleEvent event, OutputStream os) throws IOException
						{
							try
							{
								DOMUtils.write(out.getMessage(),os);
							}
							catch (TransformerException e)
							{
								throw new IOException(e);
							}
						}
					}
				);
			}
  	}
		message.setPayload(response);
		return message;
	}

	private String getHeader(MuleMessage message, String headerName)
	{
		for (Object name : message.getPropertyNames())
			if (headerName.equalsIgnoreCase((String)name))
				return (String)message.getProperty((String)name);
		return null;
	}

	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}
}
