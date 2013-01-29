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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.message.DefaultMessageWriter;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.MimeConfig;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class EbMSMimeServlet extends GenericServlet
{
	private static final long serialVersionUID = 1L;
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSMessageProcessor messageProcessor;

  @Override
  public void init(ServletConfig config) throws ServletException
  {
  	super.init(config);
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
		messageProcessor = (EbMSMessageProcessor)wac.getBean("messageProcessor",EbMSMessageProcessor.class);
  }
  
	@Override
	public void service(final ServletRequest request, ServletResponse response) throws ServletException, IOException
	{
	  try
		{
			EbMSMessage in = null;
			if (request.getContentType().startsWith("multipart"))
			{
				EbMSContentHandler handler = new EbMSContentHandler();
				MimeConfig mimeConfig = new MimeConfig();
			  mimeConfig.setHeadlessParsing(request.getContentType());
			  MimeStreamParser parser = new MimeStreamParser(mimeConfig);
			  parser.setContentHandler(handler);
				parser.parse(request.getInputStream());
				List<EbMSAttachment> attachments = handler.getAttachments();
				in = getEbMSMessage(attachments);
//				for (EbMSAttachment attachment : attachments)
//				{
//					logger.info(attachment.getContentId());
//					logger.info(attachment.getDataSource().getContentType());
//					logger.info(IOUtils.toString(attachment.getDataSource().getInputStream()));
//				}
			}
			else
			{
				in = getEbMSMessage(request);
			}
			EbMSBaseMessage out = messageProcessor.process(in);
			if (out == null)
			{
				((HttpServletResponse)response).setStatus(204);
			}
			else
			{
				((HttpServletResponse)response).setStatus(200);
				MessageWriter messageWriter = new DefaultMessageWriter();
				//messageWriter.writeMultipart(arg0,response.getOutputStream());
			}
		}
		catch (Exception e)
		{
			logger.error("",e);
		}
	  
	}

	private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		return dbf.newDocumentBuilder();
	}

	private EbMSMessage getEbMSMessage(ServletRequest request) throws ParserConfigurationException, SAXException, IOException, JAXBException
	{
		DocumentBuilder db = getDocumentBuilder();
		Document d = db.parse(request.getInputStream());
		return getEbMSMessage(d,new ArrayList<EbMSAttachment>());
	}

	private EbMSMessage getEbMSMessage(List<EbMSAttachment> attachments) throws ParserConfigurationException, SAXException, IOException, JAXBException
	{
		EbMSMessage result = null;
		if (attachments.size() > 0)
		{
			DocumentBuilder db = getDocumentBuilder();
			Document d = db.parse((attachments.get(0).getDataSource().getInputStream()));
			attachments.remove(0);
			result = getEbMSMessage(d,attachments);
		}
		return result;
	}

	private EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException
	{
		SignatureType signature = XMLMessageBuilder.getInstance(SignatureType.class).handle(getNode(document,"http://www.w3.org/2000/09/xmldsig#","Signature"));
		MessageHeader messageHeader = XMLMessageBuilder.getInstance(MessageHeader.class).handle(getNode(document,"MessageHeader"));
		SyncReply syncReply = XMLMessageBuilder.getInstance(SyncReply.class).handle(getNode(document,"SyncReply"));
		MessageOrder messageOrder = XMLMessageBuilder.getInstance(MessageOrder.class).handle(getNode(document,"MessageOrder"));
		AckRequested ackRequested = XMLMessageBuilder.getInstance(AckRequested.class).handle(getNode(document,"AckRequested"));
		ErrorList errorList = XMLMessageBuilder.getInstance(ErrorList.class).handle(getNode(document,"ErrorList"));
		Acknowledgment acknowledgment = XMLMessageBuilder.getInstance(Acknowledgment.class).handle(getNode(document,"Acknowledgment"));
		Manifest manifest = XMLMessageBuilder.getInstance(Manifest.class).handle(getNode(document,"Manifest"));
		StatusRequest statusRequest = XMLMessageBuilder.getInstance(StatusRequest.class).handle(getNode(document,"StatusRequest"));
		StatusResponse statusResponse = XMLMessageBuilder.getInstance(StatusResponse.class).handle(getNode(document,"StatusResponse"));
		return new EbMSMessage(null,signature,messageHeader,syncReply,messageOrder,ackRequested,errorList,acknowledgment,manifest,statusRequest,statusResponse,attachments);
	}

	private Node getNode(Document document, String tagName)
	{
		return getNode(document,"http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd",tagName);
	}

	private Node getNode(Document document, String namespace, String tagName)
	{
		NodeList nl = document.getElementsByTagNameNS(namespace,tagName);
		return nl.getLength() == 0 ? null : nl.item(0);
	}

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

}
