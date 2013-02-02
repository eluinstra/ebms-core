package nl.clockwork.ebms.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.w3c.dom.Document;

public class EbMSMessageReaderImpl implements EbMSMessageReader
{
	private String contentType;
	
	public EbMSMessageReaderImpl(String contentType)
	{
		this.contentType = contentType;
	}

	@Override
	public EbMSDocument read(InputStream in) throws EbMSProcessorException
	{
		
		try
		{
			if (contentType.startsWith("multipart"))
			{
				EbMSContentHandler handler = new EbMSContentHandler();
				parseEbMSMessage(handler,contentType,in);
				List<EbMSAttachment> attachments = handler.getAttachments();
				return getEbMSMessage(attachments);
			}
			else
				return getEbMSMessage(in);
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	private void parseEbMSMessage(EbMSContentHandler handler, String contentType, InputStream in) throws MimeException, IOException
	{
		MimeConfig mimeConfig = new MimeConfig();
	  mimeConfig.setHeadlessParsing(contentType);
	  MimeStreamParser parser = new MimeStreamParser(mimeConfig);
	  parser.setContentHandler(handler);
		parser.parse(in);
	}

	private EbMSDocument getEbMSMessage(InputStream in) throws Exception
	{
		DocumentBuilder db = DOMUtils.getDocumentBuilder();
		Document d = db.parse(in);
		return new EbMSDocument(d,new ArrayList<EbMSAttachment>());
	}

	private EbMSDocument getEbMSMessage(List<EbMSAttachment> attachments) throws Exception
	{
		EbMSDocument result = null;
		if (attachments.size() > 0)
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse((attachments.get(0).getDataSource().getInputStream()));
			attachments.remove(0);
			result = new EbMSDocument(d,attachments);
		}
		return result;
	}

}
