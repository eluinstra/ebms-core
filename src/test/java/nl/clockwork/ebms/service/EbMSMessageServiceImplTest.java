/*
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
package nl.clockwork.ebms.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import java.util.List;
import lombok.val;
import nl.clockwork.ebms.EbMSServer;
import nl.clockwork.ebms.FixedPostgreSQLContainer;
import nl.clockwork.ebms.WithFile;
import nl.clockwork.ebms.WithTemplate;
import org.eclipse.jetty.server.Server;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.thymeleaf.TemplateEngine;

@EnabledIf(expression = "#{systemProperties['spring.profiles.active'] == 'test'}")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@Testcontainers
class EbMSMessageServiceImplTest implements WithFile, WithTemplate
{
	@Container
	static final PostgreSQLContainer<?> database = new FixedPostgreSQLContainer();
	final TemplateEngine templateEngine = templateEngine();
	final String messageId = randomUUID().toString();
	Server server;
	String ackMessageId;
	String ackTimestamp;

	@BeforeAll
	void beforeAll() throws Exception
	{
		RestAssured.port = 8888;
		server = new EbMSServer().createServer();
		server.start();
	}

	@AfterAll
	void afterAll() throws Exception
	{
		server.stop();
	}

	@Test
	@Order(1)
	void insertCPA() throws Exception
	{
		val cpa = readFile("nl/clockwork/ebms/cpas/cpaStubEBF.rm.http.unsigned.sync.xml");
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","")
				.body(insertCpa(templateEngine,insertCpaContext(cpa)))
				.when()
				.port(8080)
				.request(Method.POST,"/service/cpa")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.body("Envelope.Body.insertCPAResponse.cpaId",equalTo("cpaStubEBF.rm.http.unsigned.sync"));
	}

	@Test
	@Order(2)
	void ebMSPing()
	{
		String uuid = randomUUID().toString();
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","\"ebXML\"")
				.body(ebMSPing(templateEngine,ebMSPingContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("Pong"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid));
	}

	@Test
	@Order(3)
	void ebMSMessageInvalid()
	{
		RestAssured.with()
				.header("SOAPAction","")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageContext(messageId)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(500)
				.contentType(ContentType.XML)
				.body("Envelope.Body.Fault.faultcode",endsWith("Client"))
				.body("Envelope.Body.Fault.faultstring",endsWith("Unable to process message! SOAPAction="));
	}

	@Test
	@Order(4)
	void ebMSMessage()
	{
		var response = RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageContext(messageId)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("Acknowledgment"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(messageId))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(messageId))
				.body("Envelope.Header.Acknowledgment.RefToMessageId",startsWith(messageId))
				.body("Envelope.Header.Acknowledgment.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.Acknowledgment.From.PartyId",equalTo("00000000000000000001"))
				.extract();
		ackMessageId = response.path("Envelope.Header.MessageHeader.MessageData.MessageId");
		ackTimestamp = response.path("Envelope.Header.MessageHeader.MessageData.Timestamp");
	}

	@Test
	@Order(5)
	void ebMSMessageStatus()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageStatus(templateEngine,ebMSMessageStatusContext(uuid,messageId)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("StatusResponse"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Body.StatusResponse.@eb:messageStatus",equalTo("Received"))
				.body("Envelope.Body.StatusResponse.RefToMessageId",startsWith(messageId));
	}

	@Test
	@Order(6)
	void getUnprocessedMessageIds() throws Exception
	{
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","")
				.body(getUnprocessedMessageIds(templateEngine,getUnprocessedMessageIdsContext()))
				.when()
				.port(8080)
				.request(Method.POST,"/service/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				// .body("Envelope.Body.getUnprocessedMessageIdsResponse.messageId*.length().sum()",equalTo(1))
				// .body("Envelope.Body.getUnprocessedMessageIdsResponse.messageId.collect { it.length() }.sum()",equalTo(1))
				// .body(HasXPath.hasXPath("/Envelope/Body/getUnprocessedMessageIdsResponse/messageId[text()='" + messageId + "@localhost']"))
				.body("Envelope.Body.getUnprocessedMessageIdsResponse.messageId",startsWith(messageId));
	}

	// @Test
	@Order(7)
	void getMessage() throws Exception
	{
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","")
				.body(getMessage(templateEngine,getMessageContext(messageId)))
				.when()
				.log()
				.all()
				.port(8080)
				.request(Method.POST,"/service/ebms")
				.then()
				.log()
				.all()
				.statusCode(200)
				.contentType(ContentType.XML)
				.body("Envelope.Body.getMessageResponse.messageId",startsWith(messageId));
	}

	@Test
	@Order(8)
	void processMessage() throws Exception
	{
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","")
				.body(processMessage(templateEngine,processMessageContext(messageId)))
				.when()
				.port(8080)
				.request(Method.POST,"/service/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.body("Envelope.Body.processMessageResponse",Matchers.equalTo(""));
	}

	// @Test
	@Order(9)
	void ebMSMessageStatusAgain()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageStatus(templateEngine,ebMSMessageStatusContext(uuid,messageId)))
				.when()
				.log()
				.all()
				.request(Method.POST,"/ebms")
				.then()
				.log()
				.all()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("StatusResponse"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Body.StatusResponse.@eb:messageStatus",equalTo("Processed"))
				.body("Envelope.Body.StatusResponse.RefToMessageId",startsWith(messageId));
	}

	// @Test
	@Order(10)
	void getUnprocessedMessageIdsAgain() throws Exception
	{
		RestAssured.with()
				.header("Content-Type","text/xml; charset=UTF-8")
				.header("SOAPAction","")
				.body(getUnprocessedMessageIds(templateEngine,getUnprocessedMessageIdsContext()))
				.when()
				.log()
				.all()
				.port(8080)
				.request(Method.POST,"/service/ebms")
				.then()
				.log()
				.all()
				.statusCode(200)
				.contentType(ContentType.XML)
				// .body("Envelope.Body.getUnprocessedMessageIdsResponse.messageId*.length().sum()",equalTo(1))
				// .body("Envelope.Body.getUnprocessedMessageIdsResponse.messageId.collect { it.length() }.sum()",equalTo(1))
				// .body(HasXPath.hasXPath("/Envelope/Body/getUnprocessedMessageIdsResponse/messageId[text()='" + messageId + "@localhost']"))
				.body("Envelope.Body.getUnprocessedMessageIdsResponse",equalTo(""));
	}

	@Test
	@Order(11)
	void ebMSMessageDuplicate()
	{
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageContext(messageId)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("Acknowledgment"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(messageId))
				.body("Envelope.Header.MessageHeader.MessageData.MessageId",startsWith(ackMessageId))
				.body("Envelope.Header.MessageHeader.MessageData.Timestamp",startsWith(ackTimestamp))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(messageId))
				.body("Envelope.Header.Acknowledgment.RefToMessageId",startsWith(messageId))
				.body("Envelope.Header.Acknowledgment.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.Acknowledgment.From.PartyId",equalTo("00000000000000000001"));
	}

	@Test
	@Order(12)
	void ebMSMessageInvalidMessageHeaderVersion()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidMessageHeaderVersion(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/@version"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	@Test
	@Order(13)
	void ebMSMessageInvalidCPAId()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidCPAIdContext(uuid,"cpaId")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(500)
				.contentType(ContentType.XML)
				.body("Envelope.Body.Fault.faultcode",endsWith("Client"))
				.body("Envelope.Body.Fault.faultstring",endsWith("CPA cpaId not found!"));
	}

	@Test
	@Order(14)
	void ebMSMessageInvalidFromPartyId()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidFromPartyIdContext(uuid,"fromPartyId")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("fromPartyId"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/From/PartyId"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(15)
	void ebMSMessageInvalidFromPartyType()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidFromPartyTypeContext(uuid,"type")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("type"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("type"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/From/PartyId"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(16)
	void ebMSMessageMissingFromPartyId()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingFromPartyId(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(500)
				.header("Content-Length",equalTo("0"));
	}

	@Test
	@Order(17)
	void ebMSMessageInvalidFromRole()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidFromRoleContext(uuid,"fromRole")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/Action"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(18)
	void ebMSMessageMissingFromRole()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingFromRole(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/From/Role"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	@Test
	@Order(19)
	void ebMSMessageInvalidToPartyId()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidToPartyIdContext(uuid,"toPartyId")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("toPartyId"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/To/PartyId"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(20)
	void ebMSMessageInvalidToPartyType()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidToPartyType(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("type"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/To/PartyId"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(21)
	void ebMSMessageInvalidToRole()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidToRoleContext(uuid,"toRole")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/Action"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(22)
	void ebMSMessageInvalidService()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidServiceContext(uuid,"service")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/Action"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(23)
	void ebMSMessageInvalidServiceType()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidServiceTypeContext(uuid,"serviceType")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/Action"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(24)
	void ebMSMessageInvalidAction()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageInvalidActionContext(uuid,"action")))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/Action"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(25)
	void ebMSMessageInvalidRefToMessageId()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidRefToMessageId(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("ValueNotRecognized"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/MessageData/RefToMessageId"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Value not found."));
	}

	@Test
	@Order(26)
	void ebMSMessageTimeToLiveExpired()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessage(templateEngine,ebMSMessageTimeToLiveExpiredContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("TimeToLiveExpired"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/MessageData/TimeToLive"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"));
	}

	@Test
	@Order(27)
	void ebMSMessageMissingDuplicateElimination()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingDuplicateElimination(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageHeader/DuplicateElimination"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Wrong value."));
	}

	@Test
	@Order(28)
	void ebMSMessageMissingAckRequested()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingAckRequested(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/AckRequested"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Wrong value."));
	}

	@Test
	@Order(29)
	void ebMSMessageInvalidAckRequestedVersion()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidAckRequestedVersion(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/AckRequested/@version"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	@Test
	@Order(30)
	void ebMSMessageNextMshAckRequested()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageNextMshAckRequested(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("NotSupported"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/AckRequested/@actor"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("NextMSH not supported."));
	}

	@Test
	@Order(31)
	void ebMSMessageInvalidAckRequestedActor()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidAckRequestedActor(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/AckRequested/@actor"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	@Test
	@Order(32)
	void ebMSMessageInvalidAckRequestedSigned()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidAckRequestedSigned(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/AckRequested/@signed"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Wrong value."));
	}

	@Test
	@Order(33)
	void ebMSMessageMissingSyncReply()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingSyncReply(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/SyncReply"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Wrong value."));
	}

	@Test
	@Order(34)
	void ebMSMessageInvalidSyncReplyVersion()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidSyncReplyVersion(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/SyncReply/@version"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	@Test
	@Order(35)
	void ebMSMessageInvalidSyncReplyActor()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidSyncReplyActor(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/SyncReply/@actor"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Wrong value."));
	}

	@Test
	@Order(36)
	void ebMSMessageMessageOrder()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMessageOrder(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("NotSupported"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Header/MessageOrder"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("MessageOrder not supported."));
	}

	@Test
	@Order(37)
	void ebMSMessageMissingManifest()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingManifest(templateEngine,ebMSMessageContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("Acknowledgment"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.Acknowledgment.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.Acknowledgment.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.Acknowledgment.From.PartyId",equalTo("00000000000000000001"));
	}

	@Test
	@Order(38)
	void ebMSMessageInvalidManifestVersion()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageInvalidManifestVersion(templateEngine,ebMSMessageContextWithAttachments(uuid,List.of("1"))))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("Inconsistent"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Body/Manifest/@version"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("Invalid value."));
	}

	// @Test
	@Order(39)
	void ebMSMessageInvalidCID()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageWithAttachments(templateEngine,ebMSMessageContextWithAttachments(uuid,List.of("1"))))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("MimeProblem"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("//Body/Manifest/Reference[@href='" + uuid + "@localhost']"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("URI cannot be resolved."));
	}

	@Test
	@Order(40)
	void ebMSMessageUnknownCID()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageWithAttachments(templateEngine,ebMSMessageContextWithAttachments(uuid,List.of("cid"))))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.log()
				.all()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("MimeProblem"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("cid:cid"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("MIME part not found."));
	}

	@Test
	@Order(41)
	void ebMSMessageMissingReference()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageMissingReference(templateEngine,ebMSMessageContextWithAttachments(uuid,List.of("cid"))))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.log()
				.all()
				.statusCode(500)
				.header("Content-Length",equalTo("0"));
	}

	// @Test
	@Order(42)
	void ebMSMessageInvalidReference()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSMessageWithAttachments(templateEngine,ebMSMessageContextWithAttachments(uuid,List.of("1","cid"))))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.statusCode(200)
				.contentType(ContentType.XML)
				.header("SOAPAction","\"ebXML\"")
				.body("Envelope.Header.MessageHeader.CPAId",equalTo("cpaStubEBF.rm.http.unsigned.sync"))
				.body("Envelope.Header.MessageHeader.From.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.From.PartyId",equalTo("00000000000000000001"))
				.body("Envelope.Header.MessageHeader.To.PartyId.@eb:type",equalTo("urn:osb:oin"))
				.body("Envelope.Header.MessageHeader.To.PartyId",equalTo("00000000000000000000"))
				.body("Envelope.Header.MessageHeader.Service",equalTo("urn:oasis:names:tc:ebxml-msg:service"))
				.body("Envelope.Header.MessageHeader.Action",equalTo("MessageError"))
				.body("Envelope.Header.MessageHeader.ConversationId",equalTo(uuid))
				.body("Envelope.Header.MessageHeader.MessageData.RefToMessageId",startsWith(uuid))
				.body("Envelope.Header.ErrorList.@eb:highestSeverity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.@eb:codeContext",equalTo("urn:oasis:names:tc:ebxml-msg:service:errors"))
				.body("Envelope.Header.ErrorList.Error.@eb:errorCode",equalTo("MimeProblem"))
				.body("Envelope.Header.ErrorList.Error.@eb:location",equalTo("cid:cid"))
				.body("Envelope.Header.ErrorList.Error.@eb:severity",equalTo("Error"))
				.body("Envelope.Header.ErrorList.Error.Description",equalTo("MIME part not found."));
	}

	@Test
	@Order(43)
	void ebMSPingXXE()
	{
		var uuid = randomUUID().toString();
		RestAssured.with()
				.header("SOAPAction","\"ebXML\"")
				.header("Content-Type","text/xml; charset=UTF-8")
				.body(ebMSPingXXE(templateEngine,ebMSPingContext(uuid)))
				.when()
				.request(Method.POST,"/ebms")
				.then()
				.log()
				.all()
				.statusCode(500)
				.header("Content-Length",equalTo("0"));
	}

}
