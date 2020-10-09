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
package nl.clockwork.ebms;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.API.run;
import static io.vavr.Predicates.instanceOf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
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

import lombok.val;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.jaxb.EbMSNamespaceMapper;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.util.DOMUtils;

public class EbMSMessageUtils
{
	public static EbMSBaseMessage getEbMSMessage(Document document) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		return getEbMSMessage(document,Collections.emptyList());
	}

	public static EbMSBaseMessage getEbMSMessage(EbMSDocument document) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		return getEbMSMessage(document.getMessage(),document.getAttachments());
	}

	private static EbMSBaseMessage getEbMSMessage(Document document, List<EbMSAttachment> attachments) throws JAXBException, XPathExpressionException, ParserConfigurationException, SAXException, IOException
	{
		val builder = new EbMSMessageBuilder();
		JAXBParser<Envelope> jaxbParser = JAXBParser.getInstance(
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
		val envelope = jaxbParser.handle(document);
		envelope.getHeader().getAny().forEach(e -> setEbMSMessageBuilder(builder,e));
		envelope.getBody().getAny().forEach(e -> setEbMSMessageBuilder(builder,e));
		builder.attachments(attachments);
		return builder.build();
	}

	@SuppressWarnings("unchecked")
	private static void setEbMSMessageBuilder(EbMSMessageBuilder result, Object o)
	{
		Match(o).of(
				Case($(instanceOf(MessageHeader.class)),i -> result.messageHeader(i)),
				Case($(instanceOf(SyncReply.class)),i -> result.syncReply(i)),
				Case($(instanceOf(MessageOrder.class)),i -> result.messageOrder(i)),
				Case($(instanceOf(AckRequested.class)),i -> result.ackRequested(i)),
				Case($(instanceOf(ErrorList.class)),i -> result.errorList((ErrorList)i)),
				Case($(instanceOf(Acknowledgment.class)),i -> result.acknowledgment(i)),
				Case($(instanceOf(Manifest.class)),i -> result.manifest(i)),
				Case($(instanceOf(StatusRequest.class)),i -> result.statusRequest(i)),
				Case($(instanceOf(StatusResponse.class)),i -> result.statusResponse(i)),
				Case($(instanceOf(JAXBElement.class)),i -> run(() ->
				{
					if (((JAXBElement<?>)i).getValue() instanceof SignatureType)
						result.signature(((JAXBElement<SignatureType>)i).getValue());
				})));
	}

	public static String toString(PartyId partyId)
	{
		return (partyId.getType() == null ? "" : partyId.getType() + ":") + partyId.getValue();
	}

	public static String toString(List<PartyId> partyIds)
	{
		return partyIds.stream().map(p -> toString(p)).findFirst().orElse(null);
	}

	public static String toString(Service service)
	{
		return CPAUtils.toString(service.getType(),service.getValue());
	}

	public static EbMSDocument getEbMSDocument(EbMSBaseMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		return EbMSDocument.builder()
			.message(EbMSMessageUtils.createSOAPMessage(message))
			.build();
	}
	
	public static EbMSDocument getEbMSDocument(EbMSMessage message) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		return EbMSDocument.builder()
			.contentId(message.getContentId())
			.message(EbMSMessageUtils.createSOAPMessage(message))
			.attachments(message.getAttachments())
			.build();
	}
	
	public static SyncReply createSyncReply(DeliveryChannel channel)
	{
		switch (channel.getMessagingCharacteristics().getSyncReplyMode())
		{
			case MSH_SIGNALS_ONLY:
			case SIGNALS_ONLY:
			case SIGNALS_AND_RESPONSE:
				SyncReply syncReply = new SyncReply();
				syncReply.setVersion(Constants.EBMS_VERSION);
				syncReply.setMustUnderstand(true);
				syncReply.setActor(Constants.NSURI_SOAP_NEXT_ACTOR);
				return syncReply;
			default:
				break;
		}
		return null;
	}

	public static Manifest createManifest()
	{
		val result = new Manifest();
		result.setVersion(Constants.EBMS_VERSION);
		return result;
	}
	
	public static ErrorList createErrorList()
	{
		val result = new ErrorList();
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
		val result = new Error();
		result.setCodeContext(EbMSAction.EBMS_SERVICE_URI + ":errors");
		result.setLocation(location);
		result.setErrorCode(errorCode.getErrorCode());
		if (!StringUtils.isEmpty(description))
		{
			result.setDescription(new Description());
			result.getDescription().setLang(language);
			result.getDescription().setValue(description);
		}
		result.setSeverity(severity);
		return result;
	}
	
	public static StatusRequest createStatusRequest(String refToMessageId) throws DatatypeConfigurationException
	{
		val result = new StatusRequest();
		result.setVersion(Constants.EBMS_VERSION);
		result.setRefToMessageId(refToMessageId);
		return result;
	}

	public static StatusResponse createStatusResponse(StatusRequest statusRequest, EbMSMessageStatus status, Instant timestamp) throws DatatypeConfigurationException
	{
		val result = new StatusResponse();
		result.setVersion(Constants.EBMS_VERSION);
		result.setRefToMessageId(statusRequest.getRefToMessageId());
		if (status != null)
		{
			result.setMessageStatus(status.getStatusCode());
			if (MessageStatusType.RECEIVED.equals(status.getStatusCode()) || MessageStatusType.PROCESSED.equals(status.getStatusCode()))
				result.setTimestamp(timestamp);
		}
		return result;
	}

	public static Reference createReference(String contentId)
	{
		val result = new Reference();
		result.setHref(Constants.CID + contentId);
		result.setType("simple");
		//reference.setRole("XLinkRole");
		return result;
	}

	public static Document createSOAPMessage(EbMSBaseMessage ebMSMessage) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val envelope = new Envelope();
		envelope.setHeader(new Header());
		envelope.setBody(new Body());
		
		envelope.getHeader().getAny().add(ebMSMessage.getMessageHeader());

		if (ebMSMessage instanceof EbMSMessage)
		{
			envelope.getHeader().getAny().add(((EbMSMessage)ebMSMessage).getSyncReply());
			envelope.getHeader().getAny().add(((EbMSMessage)ebMSMessage).getMessageOrder());
			envelope.getHeader().getAny().add(((EbMSMessage)ebMSMessage).getAckRequested());
			envelope.getBody().getAny().add(((EbMSMessage)ebMSMessage).getManifest());
		}
		else if (ebMSMessage instanceof EbMSMessageError)
			envelope.getHeader().getAny().add(((EbMSMessageError)ebMSMessage).getErrorList());
		else if (ebMSMessage instanceof EbMSAcknowledgment)
			envelope.getHeader().getAny().add(((EbMSAcknowledgment)ebMSMessage).getAcknowledgment());
		else if (ebMSMessage instanceof EbMSStatusRequest)
			envelope.getBody().getAny().add(((EbMSStatusRequest)ebMSMessage).getStatusRequest());
		else if (ebMSMessage instanceof EbMSStatusResponse)
			envelope.getBody().getAny().add(((EbMSStatusResponse)ebMSMessage).getStatusResponse());
		
		val parser = JAXBParser.getInstance(
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
		val e = new JAXBElement<>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope);
		val is = new ByteArrayInputStream(parser.handle(e,new EbMSNamespaceMapper()).getBytes());
		return DOMUtils.getDocumentBuilder().parse(is);
	}

	public static Fault getSOAPFault(String s)
	{
		try
		{
			val envelope = JAXBParser.getInstance(Envelope.class).handle(s);
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
	
	public static Document createSOAPFault(String faultCode, String faultString) throws ParserConfigurationException, JAXBException, SAXException, IOException
	{
		val envelope = new Envelope();
		envelope.setBody(new Body());
		val fault = new Fault();
		fault.setFaultcode(new QName("http://schemas.xmlsoap.org/soap/envelope/",faultCode));
		fault.setFaultstring(faultString);
		val f = new JAXBElement<Fault>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Fault"),Fault.class,fault);
		envelope.getBody().getAny().add(f);
		return DOMUtils.getDocumentBuilder().parse(new ByteArrayInputStream(JAXBParser.getInstance(Envelope.class).handle(new JAXBElement<>(new QName("http://schemas.xmlsoap.org/soap/envelope/","Envelope"),Envelope.class,envelope)).getBytes()));
	}

}
