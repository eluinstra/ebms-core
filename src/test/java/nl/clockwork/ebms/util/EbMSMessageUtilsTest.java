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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import javax.activation.DataSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessagingCharacteristics;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SeverityType;
import org.w3._2000._09.xmldsig.SignatureType;
import org.w3c.dom.Document;

import lombok.val;
import lombok.var;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.validation.ValidationException;

@TestInstance(value = Lifecycle.PER_CLASS)
public class EbMSMessageUtilsTest
{
	@Test
	public void partyIdToString()
	{
		var partyId = new PartyId();
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
		val service = new Service();
		service.setType("B");
		service.setValue("A");
		assertEquals("B:A", EbMSMessageUtils.toString(service));
	}
	
	@Test
	public void getSoapFaultString()
	{
		assertNull(EbMSMessageUtils.getSOAPFault(""));
		val fault1 = EbMSMessageUtils.getSOAPFault("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/><SOAP-ENV:Body>" + 
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
		val msgRef1 = "ref1";
		val msgRef2 = "ref2";
		
		/* createStatusRequest */
		var timestamp = Instant.now();
		val statusRequest = EbMSMessageUtils.createStatusRequest(msgRef1);
		assertEquals(Constants.EBMS_VERSION, statusRequest.getVersion());
		assertEquals(msgRef1, statusRequest.getRefToMessageId());
		assertNull(statusRequest.getId());
		statusRequest.setId(msgRef2);

		/* createStatusResponse 1 */
		var createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.FAILED, timestamp);
		assertEquals(msgRef1, createStatusResponse.getRefToMessageId());
		assertEquals(EbMSMessageStatus.RECEIVED.getStatusCode(), createStatusResponse.getMessageStatus());
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(Constants.EBMS_VERSION, createStatusResponse.getVersion());
		
		/* createStatusResponse 2 */
		timestamp = Instant.now();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, null, timestamp);
		assertNull(createStatusResponse.getTimestamp());
		assertNull(createStatusResponse.getMessageStatus());
		assertEquals(Constants.EBMS_VERSION, createStatusResponse.getVersion());
		assertEquals(msgRef1, createStatusResponse.getRefToMessageId());
		assertNull(createStatusResponse.getId());

		/* createStatusResponse 3 */
		timestamp = Instant.now();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.RECEIVED, timestamp);
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(MessageStatusType.RECEIVED.value(), createStatusResponse.getMessageStatus().value());

		/* createStatusResponse 4 */
		timestamp = Instant.now();
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.PROCESSED, timestamp);
		assertEquals(timestamp, createStatusResponse.getTimestamp());
		assertEquals(MessageStatusType.PROCESSED.value(), createStatusResponse.getMessageStatus().value());

		/* createStatusResponse 5 */
		createStatusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.CREATED, timestamp);
		assertNull(createStatusResponse.getTimestamp());
		assertNull(createStatusResponse.getMessageStatus());
	}
	
	
	@Test
	public void createErrorList()
	{
		val errorList = EbMSMessageUtils.createErrorList();
		assertEquals(Constants.EBMS_VERSION, errorList.getVersion());
		assertEquals(SeverityType.ERROR, errorList.getHighestSeverity());
		assertTrue(errorList.isMustUnderstand());
	}
	
	
	@Test
	public void createError()
	{
		val severity = SeverityType.WARNING;
		val language = "nl";
		val errorCode = EbMSErrorCode.UNKNOWN;
		val location = "location1";
		String description = null;
		var createError = EbMSMessageUtils.createError(location, errorCode, description, language, severity);
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
		val dc = new DeliveryChannel();
		dc.setMessagingCharacteristics(new MessagingCharacteristics());
		
		/* createSyncReply 1 */
		dc.getMessagingCharacteristics().setSyncReplyMode(SyncReplyModeType.MSH_SIGNALS_ONLY);
		var createSyncReply = EbMSMessageUtils.createSyncReply(dc);
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
		val fault = EbMSMessageUtils.createSOAPFault("Server","An unexpected error occurred!");
		assertEquals("Envelope", fault.getDocumentElement().getLocalName());
		assertTrue(documentToString(fault).contains("An unexpected error occurred!"));
	}
	
	@Test
	public void createSOAPFault1() throws Exception
	{
		val e = new ValidationException("soapfault test");
		val fault = EbMSMessageUtils.createSOAPFault("Client",e.getMessage());
		assertEquals("Envelope", fault.getDocumentElement().getLocalName());
		assertTrue(documentToString(fault).contains(e.getMessage()));
	}
	
	private String documentToString(Document document) throws TransformerException
	{
		val tf = TransformerFactory.newInstance();
		val t = tf.newTransformer();
		val sw = new StringWriter();
		t.transform(new DOMSource(document), new StreamResult(sw));
		return sw.toString();
	}
	
	@Test
	public void getEbMSMessage() throws Exception
	{
		val ackRequested = new AckRequested();
		ackRequested.setActor("Actor1");
		val builder = EbMSMessage.builder()
				.messageHeader(createMessageHeader())
				.ackRequested(ackRequested)
				.manifest(new Manifest())
				.attachments(Collections.emptyList());
		val doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		
		val result = (EbMSMessage)EbMSMessageUtils.getEbMSMessage(doc);
		assertEquals("Actor1", result.getAckRequested().getActor());
		
		val builder2 = EbMSDocument.builder();
		builder2.message(doc);
		val dataSource = new DataSource()
		{
			@Override
			public OutputStream getOutputStream() throws IOException
			{
				return null;
			}
			
			@Override
			public String getName()
			{
				return null;
			}
			
			@Override
			public InputStream getInputStream() throws IOException
			{
				return null;
			}
			
			@Override
			public String getContentType()
			{
				return null;
			}
		};
		val attachment = EbMSAttachmentFactory.createEbMSAttachment("cid1",dataSource);
		builder2.attachments(Arrays.asList(attachment));
		
		val result2 = (EbMSMessage)EbMSMessageUtils.getEbMSMessage(builder2.build());
		assertEquals(1, result2.getAttachments().size());
	}

	
	@Test
	public void getEbMSDocument()
	{
		
	}
	
	@Test
	public void createEbMSMessageSOAPMessage() throws Exception
	{
		val builder = EbMSMessage.builder()
				.messageHeader(createMessageHeader())
				.signature(createSignature())
				.ackRequested(createAckRequested())
				.manifest(EbMSMessageUtils.createManifest())
				.attachments(Collections.emptyList());
		var doc = EbMSMessageUtils.createSOAPMessage(builder.build());

		var documentString = documentToString(doc);
		assertTrue(documentString.contains("AckRequested"));

		doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		documentString = documentToString(doc);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}

	@Test
	public void createEbMSMessageErrorSOAPMessage() throws Exception
	{
		val builder = EbMSMessageError.builder()
				.messageHeader(createMessageHeader())
				.signature(createSignature())
				.errorList(EbMSMessageUtils.createErrorList());
		var doc = EbMSMessageUtils.createSOAPMessage(builder.build());

		var documentString = documentToString(doc);
		assertTrue(documentString.contains("ErrorList"));
		assertTrue(documentString.contains("highestSeverity=\"Error\""));

		doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		documentString = documentToString(doc);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}

	@Test
	public void createEbMSAcknowledgmentSOAPMessage() throws Exception
	{
		val builder = EbMSAcknowledgment.builder()
				.messageHeader(createMessageHeader())
				.signature(createSignature())
				.acknowledgment(createAcknowledgment());
		var doc = EbMSMessageUtils.createSOAPMessage(builder.build());

		var documentString = documentToString(doc);
		assertTrue(documentString.contains("Acknowledgment"));

		doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		documentString = documentToString(doc);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}

	@Test
	public void createEbMSStatusRequestSOAPMessage() throws Exception
	{
		val statusRequest = EbMSMessageUtils.createStatusRequest("ref1");
		val builder = EbMSStatusRequest.builder()
				.messageHeader(createMessageHeader())
				.signature(createSignature())
				.statusRequest(statusRequest);
		var doc = EbMSMessageUtils.createSOAPMessage(builder.build());

		var documentString = documentToString(doc);
		assertTrue(documentString.contains("StatusRequest"));
		assertTrue(documentString.contains("ref1"));

		doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		documentString = documentToString(doc);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}

	@Test
	public void createSOAPMessage() throws Exception
	{
		val statusRequest = EbMSMessageUtils.createStatusRequest("ref1");
		val statusResponse = EbMSMessageUtils.createStatusResponse(statusRequest, EbMSMessageStatus.EXPIRED, Instant.now());
		val builder = EbMSStatusResponse.builder()
				.messageHeader(createMessageHeader())
				.signature(createSignature())
				.statusResponse(statusResponse);
		var doc = EbMSMessageUtils.createSOAPMessage(builder.build());

		var documentString = documentToString(doc);
		assertTrue(documentString.contains("StatusResponse"));
		assertTrue(documentString.contains("ref1"));

		doc = EbMSMessageUtils.createSOAPMessage(builder.build());
		documentString = documentToString(doc);
		assertTrue(documentString.contains("http://www.w3.org/1999/xlink"));
	}

	private MessageHeader createMessageHeader()
	{
		val result = new MessageHeader();
		result.setAction("test");
		result.setService(new Service());
		result.getService().setType("ServiceType");
		result.getService().setValue("serviceValue");
		return result;
	}
	
	private SignatureType createSignature()
	{
		val result = new SignatureType();
		result.setId("signature");
		return result;
	}

	private Acknowledgment createAcknowledgment()
	{
		val result = new Acknowledgment();
		result.setActor("actor1");
		result.setMustUnderstand(true);
		result.setVersion(Constants.EBMS_VERSION);
		return result;
	}

	private AckRequested createAckRequested()
	{
		val result = new AckRequested();
		result.setVersion(Constants.EBMS_VERSION);
		result.setActor("actor2");
		result.setMustUnderstand(true);
		return result;
	}

}
