package nl.clockwork.ebms.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.processor.EbMSProcessingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EbMSMessageReader
{
	private HttpURLConnection connection;
	private InputStream input;
	
	public EbMSMessageReader(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public EbMSDocument read() throws IOException, EbMSProcessorException
	{
		try
		{
			input = connection.getInputStream();
			String statusCode = new Integer(connection.getResponseCode()).toString();
			if (statusCode.startsWith("2"))
			{
				EbMSDocument result = getEbMSMessage(input);
				return result;
			}
			else if (statusCode.startsWith("4") || statusCode.startsWith("5"))
				throw new EbMSProcessingException("StatusCode: " + statusCode + "\n" + IOUtils.toString(connection.getErrorStream()));
			else
				throw new EbMSProcessingException("StatusCode: " + statusCode);
		}
		catch (IOException e)
		{
			try
			{
				InputStream error = new BufferedInputStream(connection.getErrorStream());
				error.close();
			}
			catch (IOException ex)
			{
			}
			throw e;
		}
		catch (ParserConfigurationException e)
		{
			throw new EbMSProcessorException(e);
		}
		catch (SAXException e)
		{
			throw new EbMSProcessorException(e);
		}
		finally
		{
			if (input != null)
				input.close();
		}
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws ParserConfigurationException, SAXException, IOException
	{
		EbMSDocument result = null;
		String message = IOUtils.toString(in,getCharSet());
		if (StringUtils.isNotBlank(message))
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse(new InputSource(new StringReader(message)));
			result = new EbMSDocument(d,new ArrayList<EbMSAttachment>());
		}
		return result;
	}
	
	private String getCharSet()
	{
		String contentType = getHeaderField(connection,"Content-Type");
		String charset = null;
		for (String param: contentType.replace(" ","").split(";"))
		{
			if (param.startsWith("charset="))
			{
				charset = param.split("=",2)[1];
				break;
			}
		}
		return charset;
	}
	
	private String getHeaderField(HttpURLConnection connection, String name)
	{
		String result = connection.getHeaderField(name);
		if (result == null)
			for (String key : connection.getHeaderFields().keySet())
				if (key.equalsIgnoreCase(name))
				{
					result = connection.getHeaderField(key);
					break;
				}
		return result;
	}
}
