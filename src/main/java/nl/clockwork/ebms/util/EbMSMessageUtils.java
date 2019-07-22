/**
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
 */
package nl.clockwork.ebms.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Description;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Error;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Reference;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xmlsoap.schemas.soap.envelope.Body;
import org.xmlsoap.schemas.soap.envelope.Envelope;
import org.xmlsoap.schemas.soap.envelope.Fault;
import org.xmlsoap.schemas.soap.envelope.Header;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSErrorCode;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.xml.EbMSNamespaceMapper;

public class EbMSMessageUtils
{
	private static boolean oraclePatch;

	public static EbMSMessage getEbMSMessage(Document document) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		return getEbMSMessage(document, new ArrayList<EbMSAttachment>());
	}

	public static EbMSMessage getEbMSMessage(EbMSDocument document) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		return getEbMSMessage(document.getMessage(),document.getAttachments());
	}

	@SuppressWarnings("unchecked")
	private static EbMSMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		EbMSMessage result = new EbMSMessage();
		result.setMessage(document);
		result.setAttachments(attachments);

		JAXBParser<Envelope> messageBuilder = JAXBParser.getInstance(
				Envelope.class,
				Envelope.class,
				MessageHeader.class,
				SyncReply.class,
				MessageOrder.class,
				AckRequested.class,
				SignatureType.class,
				ErrorList.class,
				Acknowledgment.class,
				Manifest.class,
				StatusRequest.class,
				StatusResponse.class);
		Envelope envelope = messageBuilder.handle(document);

		envelope.getHeader().getAny().stream().forEach(e ->
		{
			if (e instanceof JAXBElement && ((JAXBElement<?>)e).getValue() instanceof SignatureType)
				result.setSignature(((JAXBElement<SignatureType>)e).getValue());
			else if (e instanceof MessageHeader)
				result.setMessageHeader((MessageHeader)e);
			else if (e instanceof SyncReply)
				result.setSyncReply((SyncReply)e);
			else if (e instanceof MessageOrder)
				result.setMessageOrder((MessageOrder)e);
			else if (e instanceof AckRequested)
				result.setAckRequested((AckRequested)e);
			else if (e instanceof ErrorList)
				result.setErrorList((ErrorList)e);
			else if (e instanceof Acknowledgment)
				result.setAcknowledgment((Acknowledgment)e);
		});

		envelope.getBody().getAny().stream().forEach(e ->
		{
			if (e instanceof Manifest)
				result.setManifest((Manifest)e);
			else if (e instanceof StatusRequest)
				result.setStatusRequest((StatusRequest)e);
			else if (e instanceof StatusResponse)
				result.setStatusResponse((StatusResponse)e);
		});

		return result;
	}

	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(Service service)
	{
		return CPAUtils.toString(service.getType(),service.getValue());
	}

	public static EbMSDocument getEbMSDocument(EbMSMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		return new EbMSDocument(message.getContentId(),message.getMessage(),message.getAttachments());
	}
	
	public static SyncReply createSyncReply(DeliveryChannel channel)
	{
		SyncReply reply = null;
		switch (channel.getMessagingCharacteristics().getSyncReplyMode())
		{
			case MSH_SIGNALS_ONLY:
			case SIGNALS_ONLY:
			case SIGNALS_AND_RESPONSE:
				SyncReply syncReply = new SyncReply();
				syncReply.setVersion(Constants.EBMS_VERSION);
				syncReply.setMustUnderstand(true);
				syncReply.setActor(Constants.NSURI_SOAP_NEXT_ACTOR);
				reply = syncReply;
			default:
				break;
		}

		return reply;
	}

	public static Manifest createManifest()
	{
		Manifest manifest = new Manifest();
		manifest.setVersion(Constants.EBMS_VERSION);
		return manifest;
	}
	
	public static ErrorList createErrorList()
	{
		ErrorList result = new ErrorList();
		result.setVersion(Constants.EBMS_VERSION);
		result.setMustUnderstand(true);
		result.setHighestSeverity(SeverityType.ERROR);
		return result;
	}
	
	public static Error createError(String location, EbMSErrorCode errorCode, String description)
	{
		return createError(location,errorCode,description,Constants.EBMS_DEFAULT_LANGUAGE,SeverityType.ERROR);
	}
	
	public static Error createError(String location, EbMSErrorCode errorCode, String description, String language, SeverityType severity)
	{
		Error error = new Error();
		error.setCodeContext(Constants.EBMS_SERVICE_URI + ":errors");
		error.setLocation(location);
		error.setErrorCode(errorCode.errorCode());
		if (!StringUtils.isEmpty(description))
		{
			error.setDescription(new Description());
			error.getDescription().setLang(language);
			error.getDescription().setValue(description);
		}
		error.setSeverity(severity);
		return error;
	}
	
	public static StatusRequest createStatusRequest(String refToMessageId) throws DatatypeConfigurationException
	{
		StatusRequest request = new StatusRequest();
		request.setVersion(Constants.EBMS_VERSION);
		request.setRefToMessageId(refToMessageId);
		return request;
	}

	public static StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, Date timestamp) throws DatatypeConfigurationException
	{
		StatusResponse response = new StatusResponse();
		response.setVersion(Constants.EBMS_VERSION);
		response.setRefToMessageId(statusRequest.getRefToMessageId());
		if (status != null)
		{
			response.setMessageStatus(status.statusCode());
			if (MessageStatusType.RECEIVED.equals(status.statusCode()) || MessageStatusType.PROCESSED.equals(status.statusCode()))
				response.setTimestamp(timestamp);
		}
		return response;
	}

	public static Reference createReference(String contentId)
	{
		Reference reference = new Reference();
		reference.setHref(Constants.CID + contentId);
		reference.setType("simple");
		//reference.setRole("XLinkRole");
		return reference;
	}

	public static Document createSOAPMessage(EbMSMessage ebMSMessage) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		Envelope envelope = new Envelope();
		envelope.setHeader(new Header());
		envelope.setBody(new Body());
		
		envelope.getHeader().getAny().add(ebMSMessage.getMessageHeader());
		envelope.getHeader().getAny().add(ebMSMessage.getSyncReply());
		envelope.getHeader().getAny().add(ebMSMessage.getMessageOrder());
		envelope.getHeader().getAny().add(ebMSMessage.getAckRequested());
		envelope.getHeader().getAny().add(ebMSMessage.getErrorList());
		envelope.getHeader().getAny().add(ebMSMessage.getAcknowledgment());
		envelope.getBody().getAny().add(ebMSMessage.getManifest());
		envelope.getBody().getAny().add(ebMSMessage.getStatusRequest());
		envelope.getBody().getAny().add(ebMSMessage.getStatusResponse());
		
		JAXBParser<Envelope> messageBuilder = JAXBParser.getInstance(
				Envelope.class,
				Envelope.class,
				MessageHeader.class,
				SyncReply.class,
				MessageOrder.class,
				AckRequested.class,
				ErrorList.class,
				Acknowledgment.class,
				Manifest.class,
				StatusRequest.class,
				StatusResponse.class);
//		JAXBElement<Envelope> e = new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope);
//		ByteArrayInputStream is = new ByteArrayInputStream(messageBuilder.handle(e).getBytes());
//		return DOMUtils.getDocumentBuilder().parse(is);
		NamespacePrefixMapper namespacePrefixMapper = oraclePatch ? new EbMSNamespaceMapper() : null;
		JAXBElement<Envelope> e = new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope);
		ByteArrayInputStream is = new ByteArrayInputStream(messageBuilder.handle(e,namespacePrefixMapper).getBytes());
		return DOMUtils.getDocumentBuilder().parse(is);
	}

	public static Fault getSOAPFault(String s)
	{
		try
		{
			Envelope envelope = JAXBParser.getInstance(Envelope.class).handle(s);
			if (envelope != null)
				return getSOAPFault(envelope);
		}
		catch (JAXBException e)
		{
			// ignore error
		}
		return null;
	}
	
	public static Fault getSOAPFault(Envelope envelope)
	{
		if (envelope.getBody() != null /*&& envelope.getBody().getAny() != null*/)
			return envelope.getBody().getAny().stream()
					.filter(e -> ((JAXBElement<?>)e).getDeclaredType().equals(Fault.class))
					.map(e -> (Fault)((JAXBElement<?>)e).getValue())
					.findFirst()
					.orElse(null);
		return null;
	}
	
	public static Document createSOAPFault(Exception e) throws ParserConfigurationException, JAXBException, SAXException, IOException
	{
		Envelope envelope = new Envelope();
		envelope.setBody(new Body());
		Fault fault = new Fault();
		fault.setFaultcode(new QName("http://schemas.xmlsoap.org/soap/envelope/","Server"));
		fault.setFaultstring(e.getMessage());
		//fault.setDetail(new Detail());
		//JAXBElement<String> f = new JAXBElement<String>(new QName("","String"),String.class,ExceptionUtils.getStackTrace(e));
		//fault.getDetail().getAny().add(f);
		JAXBElement<Fault> f = new JAXBElement<Fault>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Fault"),Fault.class,fault);
		envelope.getBody().getAny().add(f);

		return DOMUtils.getDocumentBuilder().parse(new ByteArrayInputStream(JAXBParser.getInstance(Envelope.class).handle(new JAXBElement<Envelope>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope)).getBytes()));
	}
	
	public static void setOraclePatch(boolean oraclePatch)
	{
		EbMSMessageUtils.oraclePatch = oraclePatch;
	}
}
