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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessagingCharacteristics;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Error;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusRequest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusResponse;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.w3._2000._09.xmldsig.SignatureType;
import org.w3c.dom.Document;
import org.xmlsoap.schemas.soap.envelope.Fault;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSErrorCode;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;

public class EbMSMessageUtilsTest
{

	@Test
	public void partyIdToString()
	{
		PartyId partyId = new PartyId();
		partyId.setType("B");
		partyId.setValue("A");
		assertEquals("B:A", EbMSMessageUtils.toString(partyId));
		
		partyId = new PartyId();
		partyId.setValue("A");
		assertEquals("A", EbMSMessageUtils.toString(partyId));
	}

	@Test
	public void serviceToString()
	{
		Service service = new Service();
		service.setType("B");
		service.setValue("A");
		assertEquals("B:A", EbMSMessageUtils.toString(service));
	}
	
	@Test
	public void getSoapFaultString()
	{
		assertNull(EbMSMessageUtils.getSOAPFault(""));
		Fault fault1 = EbMSMessageUtils.getSOAPFault("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body>" + 
				"<SOAP-ENV:Fault><faultcode>SOAP-ENV:Client</faultcode>" + 
				"<faultstring>Message does not have necessary info</faultstring>" + 
				"<faultactor>http://gizmos.com/failure</faultactor><detail></detail></SOAP-ENV:Fault></SOAP-ENV:Body></SOAP-ENV:Envelope>");
		assertEquals("Client", fault1.getFaultcode().getLocalPart());
		assertEquals("http://gizmos.com/failure", fault1.getFaultactor());
		assertEquals("Message does not have necessary info", fault1.getFaultstring());
		
		assertNull(EbMSMessageUtils.getSOAPFault("<xml/>"));
	}
	
	@Test
	public void createStatusResponse() throws DatatypeConfigurationException
	{
		String msgRef1 = "ref1";
		String msgRef2 = "ref2";
		
		/* createStatusRequest */
		Date timestamp = new Date();
		StatusRequest statusRequest = EbMSMessageUtils.createStatusRequest(msgRef1);
		assertEquals(Constants.EBMS_VERSION, statusRequest.getVersion());
		assertEquals(msgRef1, statusRequest.getRefToMessageId());
		assertNull(statusRequest.getId());
		statusRequest.setId(msgRef2);

		/* createStatusResponse 1 */
		StatusResponse createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.FAILED, timestamp);
		assertEquals(msgRef1, createStatusResponse.getRefToMessageId());
		assertEquals(EbMSMessageStatus.RECEIVED.statusCode(), createStatusResponse.getMessageStatus());
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(Constants.EBMS_VERSION, createStatusResponse.getVersion());
		
		/* createStatusResponse 2 */
		timestamp = new Date();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, null, timestamp);
		assertNull(createStatusResponse.getTimestamp());
		assertNull(createStatusResponse.getMessageStatus());
		assertEquals(Constants.EBMS_VERSION, createStatusResponse.getVersion());
		assertEquals(msgRef1, createStatusResponse.getRefToMessageId());
		assertNull(createStatusResponse.getId());

		/* createStatusResponse 3 */
		timestamp = new Date();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.RECEIVED, timestamp);
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(MessageStatusType.RECEIVED.value(), createStatusResponse.getMessageStatus().value());

		/* createStatusResponse 4 */
		timestamp = new Date();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.PROCESSED, timestamp);
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(MessageStatusType.PROCESSED.value(), createStatusResponse.getMessageStatus().value());

		/* createStatusResponse 5 */
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.SENDING, timestamp);
		assertNull(createStatusResponse.getTimestamp());
		assertNull(createStatusResponse.getMessageStatus());
	}
	
	
	@Test
	public void createErrorList()
	{
		ErrorList errorList = EbMSMessageUtils.createErrorList();
		assertEquals(Constants.EBMS_VERSION, errorList.getVersion());
		assertEquals(SeverityType.ERROR, errorList.getHighestSeverity());
		assertTrue(errorList.isMustUnderstand());
	}
	
	
	@Test
	public void createError()
	{
		SeverityType severity = SeverityType.WARNING;
		String language = "nl";
		EbMSErrorCode errorCode = EbMSErrorCode.UNKNOWN;
		String location = "location1";
		String description = null;
		Error createError = EbMSMessageUtils.createError(location, errorCode, description, language, severity);
		assertEquals(0, createError.getAny().size());
		assertEquals(location, createError.getLocation());
		assertNull(createError.getDescription());
		assertEquals(severity, createError.getSeverity());
		
		description = "to err is human";
		createError = EbMSMessageUtils.createError(location, errorCode, description, language, severity);
		assertEquals(description, createError.getDescription().getValue());
		assertEquals(language, createError.getDescription().getLang());
		
		
		createError = EbMSMessageUtils.createError(location, errorCode, description);
		assertEquals(Constants.EBMS_DEFAULT_LANGUAGE, createError.getDescription().getLang());
		assertEquals(SeverityType.ERROR, createError.getSeverity());

	}

	
	@Test
	public void createSyncReply()
	{
		DeliveryChannel dc = new DeliveryChannel();
		dc.setMessagingCharacteristics(new MessagingCharacteristics());
		
		/* createSyncReply 1 */
		dc.getMessagingCharacteristics().setSyncReplyMode(SyncReplyModeType.MSH_SIGNALS_ONLY);
		SyncReply createSyncReply = EbMSMessageUtils.createSyncReply(dc);
		assertEquals(Constants.EBMS_VERSION, createSyncReply.getVersion());
		assertEquals(Constants.NSURI_SOAP_NEXT_ACTOR, createSyncReply.getActor());
		assertTrue(createSyncReply.isMustUnderstand());

		/* createSyncReply 2 */
		dc.getMessagingCharacteristics().setSyncReplyMode(SyncReplyModeType.SIGNALS_ONLY);
		createSyncReply = EbMSMessageUtils.createSyncReply(dc);
		assertNotNull(createSyncReply);

		/* createSyncReply 3 */
		dc.getMessagingCharacteristics().setSyncReplyMode(SyncReplyModeType.SIGNALS_AND_RESPONSE);
		createSyncReply = EbMSMessageUtils.createSyncReply(dc);
		assertNotNull(createSyncReply);

		/* createSyncReply 4 */
		dc.getMessagingCharacteristics().setSyncReplyMode(SyncReplyModeType.NONE);
		createSyncReply = EbMSMessageUtils.createSyncReply(dc);
		assertNull(createSyncReply);
	}
	
	@Test
	public void createSOAPFault() throws Exception
	{
		Exception e = new IOException("soapfault test");
		Document fault = EbMSMessageUtils.createSOAPFault(e);
		assertEquals("Envelope", fault.getDocumentElement().getLocalName());
		assertTrue(documentToString(fault).contains(e.getMessage()));
	}
	
	private String documentToString(Document document) throws TransformerException
	{
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer();
		StringWriter sw = new StringWriter();
		t.transform(new DOMSource(document), new StreamResult(sw));
		return sw.toString();
	}
	
	@Test
	public void getEbMSMessage() throws Exception
	{
		EbMSMessage ebMSMessage = new EbMSMessage();
		AckRequested ackRequested = new AckRequested();
		ackRequested.setActor("Actor1");
		ebMSMessage.setMessageHeader(new MessageHeader());
		ebMSMessage.setAckRequested(ackRequested);
		Document doc = EbMSMessageUtils.createSOAPMessage(ebMSMessage);
		
		
		EbMSMessage result = EbMSMessageUtils.getEbMSMessage(doc);
		assertEquals("Actor1", result.getAckRequested().getActor());
		
		EbMSMessage msg2 = new EbMSMessage();
		msg2.setMessage(doc);
		msg2.setAttachments(new ArrayList<>());
		javax.activation.DataSource dataSource = null;
		EbMSAttachment att = EbMSAttachmentFactory.createCachedEbMSAttachment("cid1",dataSource);
		msg2.getAttachments().add(att);
		
		EbMSMessage result2 = EbMSMessageUtils.getEbMSMessage(msg2);
		assertEquals(1, result2.getAttachments().size());
	}

	
	@Test
	public void getEbMSDocument()
	{
		
	}
	
	@Test
	public void createSOAPMessage() throws Exception
	{
		EbMSMessage ebMSMessage = new EbMSMessage();
		ebMSMessage.setMessageHeader(new MessageHeader());
		ebMSMessage.getMessageHeader().setAction("test");
		ebMSMessage.getMessageHeader().setService(new Service());
		ebMSMessage.getMessageHeader().getService().setType("ServiceType");
		ebMSMessage.getMessageHeader().getService().setValue("serviceValue");
		ebMSMessage.setSignature(new SignatureType());
		ebMSMessage.getSignature().setId("signature");
		
		ebMSMessage.setAcknowledgment(new Acknowledgment());
		ebMSMessage.getAcknowledgment().setActor("actor1");
		ebMSMessage.getAcknowledgment().setMustUnderstand(true);
		ebMSMessage.getAcknowledgment().setVersion(Constants.EBMS_VERSION);
		
		ebMSMessage.setAckRequested(new AckRequested());
		ebMSMessage.getAckRequested().setVersion(Constants.EBMS_VERSION);
		ebMSMessage.getAckRequested().setActor("actor2");
		ebMSMessage.getAckRequested().setMustUnderstand(true);
		
		ebMSMessage.setErrorList(EbMSMessageUtils.createErrorList());
		ebMSMessage.setManifest(EbMSMessageUtils.createManifest());
		ebMSMessage.setStatusRequest(EbMSMessageUtils.createStatusRequest("ref1"));
		ebMSMessage.setStatusResponse(EbMSMessageUtils.createStatusResponse(ebMSMessage.getStatusRequest(), EbMSMessageStatus.EXPIRED, new Date()));
		
		Document doc = EbMSMessageUtils.createSOAPMessage(ebMSMessage);

		String documentString = documentToString(doc);
		assertTrue(documentString.contains("AckRequested"));
		assertTrue(documentString.contains("ErrorList"));
		assertTrue(documentString.contains("StatusRequest"));
		assertTrue(documentString.contains("StatusResponse"));
		assertTrue(documentString.contains("ref1"));
		assertTrue(documentString.contains("highestSeverity=\"Error\""));

		EbMSMessageUtils.setOraclePatch(true);
		doc = EbMSMessageUtils.createSOAPMessage(ebMSMessage);
		documentString = documentToString(doc);
		EbMSMessageUtils.setOraclePatch(false);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}
	
	
}
