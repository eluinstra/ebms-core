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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class EbMSResponseHandler
{
	private HttpURLConnection connection;
	
	public EbMSResponseHandler(HttpURLConnection connection)
	{
		this.connection = connection;
	}

	public EbMSDocument read() throws IOException, ParserConfigurationException, SAXException, EbMSResponseException
	{
		InputStream input = null;
		try
		{
			input = connection.getInputStream();
			if (connection.getResponseCode() / 100 == 2)
			{
				return getEbMSMessage(input);
			}
			else if (connection.getResponseCode() >= 400)
			{
				InputStream errorStream = connection.getErrorStream();
				String error = IOUtils.toString(errorStream);
				errorStream.close();
				throw new EbMSResponseException(connection.getResponseCode(),error);
			}
			else
				throw new EbMSResponseException(connection.getResponseCode());
		}
		catch (IOException e)
		{
			try
			{
				connection.getResponseCode();
				InputStream errorStream = new BufferedInputStream(connection.getErrorStream());
				IOUtils.toString(errorStream);
				errorStream.close();
			}
			catch (IOException ex)
			{
			}
			throw e;
		}
		finally
		{
			if (input != null)
				input.close();
		}
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws IOException, ParserConfigurationException, SAXException
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
