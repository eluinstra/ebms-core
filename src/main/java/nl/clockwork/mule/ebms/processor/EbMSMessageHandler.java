package nl.clockwork.mule.ebms.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.server.EbMSInputStreamHandler;
import nl.clockwork.ebms.server.EbMSInputStreamHandlerImpl;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.ProtocolException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.transport.http.HttpResponse;

public class EbMSMessageHandler implements Callable
{
	private EbMSMessageProcessor ebMSMessageProcessor;

	@Override
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		MuleMessage message = eventContext.getMessage();
  	final HttpRequest request = (HttpRequest)message.getPayload();
  	final HttpResponse response = new HttpResponse();
		response.setBody(new OutputHandler()
		{
			@Override
			public void write(MuleEvent event, final OutputStream out) throws IOException
			{
				try
				{
			  	EbMSInputStreamHandler messageHandler = 
			  		new EbMSInputStreamHandlerImpl(ebMSMessageProcessor,getHeaders(request))
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
					messageHandler.handle(request.getBody());
				}
				catch (EbMSProcessorException e)
				{
					throw new IOException(e);
				}
			}
		});
		message.setPayload(response);
		return message;
	}

	private Map<String,String> getHeaders(HttpRequest request)
	{
		Map<String,String> result = new HashMap<String,String>();
		for (Header header : request.getHeaders())
			result.put(header.getName(),header.getValue());
		return result;
	}
	
	public void setEbMSMessageProcessor(EbMSMessageProcessor ebMSMessageProcessor)
	{
		this.ebMSMessageProcessor = ebMSMessageProcessor;
	}

}
