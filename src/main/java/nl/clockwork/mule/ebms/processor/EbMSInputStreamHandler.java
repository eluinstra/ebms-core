package nl.clockwork.mule.ebms.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSInputStreamHandlerImpl;
import nl.clockwork.mule.common.Callable;

import org.apache.commons.httpclient.ContentLengthInputStream;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

public class EbMSInputStreamHandler extends Callable
{
	private EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public Object onCall(final MuleMessage message) throws Exception
	{
  	final ContentLengthInputStream request = (ContentLengthInputStream)message.getPayload();
  	final HttpResponse response = new HttpResponse();
		response.setBody(
			new OutputHandler()
			{
				@Override
				public void write(MuleEvent event, final OutputStream out) throws IOException
				{
					try
					{
						nl.clockwork.ebms.server.EbMSInputStreamHandler messageHandler = 
				  		new EbMSInputStreamHandlerImpl(ebMSMessageProcessor,getHeaders(message))
							{
								@Override
								public void writeResponseStatus(int statusCode)
								{
									try
									{
										response.setStatusLine(HttpVersion.parse(HttpConstants.HTTP11),statusCode);
									}
									catch (ProtocolException e)
									{
									}
								}
								
								@Override
								public void writeResponseHeader(String name, String value)
								{
									response.addHeader(new Header(name,value));
								}
								
								@Override
								public OutputStream getOutputStream() throws IOException
								{
									return out;
								}
							}
				  	;
						messageHandler.handle(request);
					}
					catch (EbMSProcessorException e)
					{
						throw new IOException(e);
					}
				}
			}
		);
		message.setPayload(response);
		return message;
	}

	private Map<String,String> getHeaders(MuleMessage message)
	{
		Map<String,String> result = new HashMap<String,String>();
		for (Object name : message.getPropertyNames(PropertyScope.INBOUND))
			result.put((String)name,(String)message.getProperty((String)name));
		return result;
	}

	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}

}
