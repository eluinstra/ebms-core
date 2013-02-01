package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.RawEbMSMessage;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Mime4jEbMSMessageReader implements EbMSMessageReader
{

	@Override
	public RawEbMSMessage read(String contentType, InputStream in) throws EbMSProcessorException
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

	@Override
	public EbMSMessage read(RawEbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			return getEbMSMessage(message.getMessage(),message.getAttachments());
		}
		catch (JAXBException e)
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

	private RawEbMSMessage getEbMSMessage(InputStream in) throws Exception
	{
		DocumentBuilder db = DOMUtils.getDocumentBuilder();
		Document d = db.parse(in);
		return new RawEbMSMessage(d,new ArrayList<EbMSAttachment>());
	}

	private RawEbMSMessage getEbMSMessage(List<EbMSAttachment> attachments) throws Exception
	{
		RawEbMSMessage result = null;
		if (attachments.size() > 0)
		{
			DocumentBuilder db = DOMUtils.getDocumentBuilder();
			Document d = db.parse((attachments.get(0).getDataSource().getInputStream()));
			attachments.remove(0);
			result = new RawEbMSMessage(d,attachments);
		}
		return result;
	}

	private EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException
	{
		//TODO: optimize
		MessageHeader messageHeader = XMLMessageBuilder.getInstance(MessageHeader.class).handle(getNode(document,"MessageHeader"));
		SyncReply syncReply = XMLMessageBuilder.getInstance(SyncReply.class).handle(getNode(document,"SyncReply"));
		MessageOrder messageOrder = XMLMessageBuilder.getInstance(MessageOrder.class).handle(getNode(document,"MessageOrder"));
		AckRequested ackRequested = XMLMessageBuilder.getInstance(AckRequested.class).handle(getNode(document,"AckRequested"));
		ErrorList errorList = XMLMessageBuilder.getInstance(ErrorList.class).handle(getNode(document,"ErrorList"));
		Acknowledgment acknowledgment = XMLMessageBuilder.getInstance(Acknowledgment.class).handle(getNode(document,"Acknowledgment"));
		Manifest manifest = XMLMessageBuilder.getInstance(Manifest.class).handle(getNode(document,"Manifest"));
		StatusRequest statusRequest = XMLMessageBuilder.getInstance(StatusRequest.class).handle(getNode(document,"StatusRequest"));
		StatusResponse statusResponse = XMLMessageBuilder.getInstance(StatusResponse.class).handle(getNode(document,"StatusResponse"));
		return new EbMSMessage(messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}

	private Node getNode(Document document, String tagName)
	{
		return DOMUtils.getNode(document,"http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd",tagName);
	}

}
