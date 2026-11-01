/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.api.ebms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import io.restassured.config.MultiPartConfig;
import io.restassured.http.ContentType;
import io.restassured.http.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.val;
import nl.clockwork.ebms.EbMSServer;
import nl.clockwork.ebms.WithFile;
import nl.clockwork.ebms.WithRestAssured;
import nl.clockwork.ebms.WithSignedMessage;
import nl.clockwork.ebms.WithTemplate;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * End-to-end tests for signed (and encrypted) EbMS messages posted to the {@code /ebms} endpoint. Two CPAs are
 * exercised:
 * <ul>
 * <li>{@code cpaStubEBF.rm.https.signed.multi}: a CPA with multiple DIGIPOORT partyIds; a signed message is accepted
 * for each partyId.</li>
 * <li>{@code cpaStubEBF.rm.https.signed.encrypted}: a CPA that requires encryption; the signed message must be
 * encrypted before posting.</li>
 * </ul>
 * Unlike {@link EbMSMessageServiceImplIT} (unsigned/sync CPA), the outbound message must be signed (and encrypted
 * for the encrypted CPA) before it is posted; the receive-side behaviour is asserted through the management API
 * because these delivery channels are async ({@code syncReplyMode="none"}), so an accepted message is stored as
 * {@code RECEIVED} with a {@code 204} response.
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
class EbMSMessageSignedIT implements WithFile, WithTemplate, WithRestAssured, WithSignedMessage
{
        private static final String MULTI_CPA_ID = "cpaStubEBF.rm.https.signed.multi";
        private static final String ENCRYPTED_CPA_ID = "cpaStubEBF.rm.https.signed.encrypted";

        private static final String PARTY_TYPE = "urn:osb:oin";
        private static final String DIGIPOORT_ROLE = "DIGIPOORT";
        private static final String OVERHEID_ROLE = "OVERHEID";
        private static final String DIGIPOORT_PARTY_ID = PARTY_TYPE + ":00000000000000000000";
        private static final String DIGIPOORT_PARTY_ID_SECOND = PARTY_TYPE + ":90000000000000000001";
        private static final String OVERHEID_PARTY_ID = PARTY_TYPE + ":00000000000000000001";
        private static final String SERVICE = "urn:osb:services:osb:afleveren:1.1$1.0";
        private static final String ACTION = "afleveren";

        final TemplateEngine templateEngine = templateEngine();
        Server server;
        String multiSecondPartyIdMessageId;
        String multiFirstPartyIdMessageId;
        String encryptedMessageId;

        @BeforeAll
        void beforeAll() throws Exception
        {
                RestAssured.port = 8888;
                RestAssured.config = RestAssured.config().multiPartConfig(MultiPartConfig.multiPartConfig().defaultSubtype("related").defaultBoundary(UUID.randomUUID().toString()));
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
        void insertMultiCPA() throws Exception
        {
                insertCPA(MULTI_CPA_ID);
        }

        @Test
        @Order(2)
        void ebMSMessageSecondPartyId() throws Exception
        {
                multiSecondPartyIdMessageId = randomMessageId();
                val wire = toWire(buildSignedEbMSMessage(MULTI_CPA_ID, multiSecondPartyIdMessageId, DIGIPOORT_PARTY_ID_SECOND, DIGIPOORT_ROLE, SERVICE, ACTION, false, null));
                postAccepted(wire);
                assertStoredReceived(multiSecondPartyIdMessageId, MULTI_CPA_ID, DIGIPOORT_PARTY_ID_SECOND);
        }

        @Test
        @Order(3)
        void ebMSMessageFirstPartyId() throws Exception
        {
                multiFirstPartyIdMessageId = randomMessageId();
                val wire = toWire(buildSignedEbMSMessage(MULTI_CPA_ID, multiFirstPartyIdMessageId, DIGIPOORT_PARTY_ID, DIGIPOORT_ROLE, SERVICE, ACTION, false, null));
                postAccepted(wire);
                assertStoredReceived(multiFirstPartyIdMessageId, MULTI_CPA_ID, DIGIPOORT_PARTY_ID);
        }

        @Test
        @Order(4)
        void insertEncryptedCPA() throws Exception
        {
                insertCPA(ENCRYPTED_CPA_ID);
        }

        @Test
        @Order(5)
        void ebMSMessageEncrypted() throws Exception
        {
                encryptedMessageId = randomMessageId();
                val wire = toWire(buildSignedEbMSMessage(
                                ENCRYPTED_CPA_ID,
                                encryptedMessageId,
                                DIGIPOORT_PARTY_ID,
                                DIGIPOORT_ROLE,
                                SERVICE,
                                ACTION,
                                true,
                                "Dit is een geheime test.".getBytes(StandardCharsets.UTF_8)));
                postAccepted(wire);
                assertStoredReceived(encryptedMessageId, ENCRYPTED_CPA_ID, DIGIPOORT_PARTY_ID);
        }

        private void insertCPA(String cpaId) throws Exception
        {
                val cpa = readFile("nl/clockwork/ebms/cpas/" + cpaId + ".xml");
                RestAssured.with()
                                .header("Content-Type", "text/xml; charset=UTF-8")
                                .header("SOAPAction", "")
                                .body(insertCpa(templateEngine, insertCpaContext(cpa)))
                                .when()
                                .port(8080)
                                .request(Method.POST, "/service/cpa")
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.XML)
                                .body("Envelope.Body.insertCPAResponse.cpaId", equalTo(cpaId));
        }

        private void postEbMSMessage(WirePayload wire, int expectedStatus)
        {
                RestAssured.with()
                                .header("Content-Type", wire.contentType())
                                .header("SOAPAction", "\"ebXML\"")
                                .body(wire.body())
                                .when()
                                .request(Method.POST, "/ebms")
                                .then()
                                .statusCode(expectedStatus);
        }

        private void postAccepted(WirePayload wire)
        {
                postEbMSMessage(wire, 204);
        }

        private void assertStoredReceived(String messageId, String cpaId, String fromPartyId)
        {
                val unprocessed = getUnprocessedMessageIds(cpaId, fromPartyId);
                assertThat(unprocessed).anyMatch(id -> id.startsWith(messageId));
                RestAssured.with()
                                .header("Content-Type", "text/xml; charset=UTF-8")
                                .header("SOAPAction", "")
                                .body(getMessage(templateEngine, getMessageContext(messageId)))
                                .when()
                                .port(8080)
                                .request(Method.POST, "/service/ebms")
                                .then()
                                .statusCode(200)
                                .contentType(ContentType.XML)
                                .body("Envelope.Body.getMessageResponse.message.properties.cpaId", equalTo(cpaId))
                                .body("Envelope.Body.getMessageResponse.message.properties.fromParty.partyId", equalTo(fromPartyId))
                                .body("Envelope.Body.getMessageResponse.message.properties.fromParty.role", equalTo(DIGIPOORT_ROLE))
                                .body("Envelope.Body.getMessageResponse.message.properties.toParty.partyId", equalTo(OVERHEID_PARTY_ID))
                                .body("Envelope.Body.getMessageResponse.message.properties.service", equalTo(SERVICE))
                                .body("Envelope.Body.getMessageResponse.message.properties.action", equalTo(ACTION))
                                .body("Envelope.Body.getMessageResponse.message.properties.messageStatus", equalTo("RECEIVED"));
        }

        private List<String> getUnprocessedMessageIds(String cpaId, String fromPartyId)
        {
                val result = RestAssured.with()
                                .header("Content-Type", "text/xml; charset=UTF-8")
                                .header("SOAPAction", "")
                                .body(getUnprocessedMessageIds(templateEngine, unprocessedMessageIdsContext(cpaId, fromPartyId)))
                                .when()
                                .port(8080)
                                .request(Method.POST, "/service/ebms")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body()
                                .path("Envelope.Body.getUnprocessedMessageIdsResponse.messageId");
                if (result == null)
                        return List.of();
                if (result instanceof List<?> list)
                        return list.stream().map(Object::toString).toList();
                return List.of(result.toString());
        }

        private Context unprocessedMessageIdsContext(String cpaId, String fromPartyId)
        {
                val result = new Context();
                result.setVariable("cpaId", cpaId);
                result.setVariable("partyType", PARTY_TYPE);
                result.setVariable("fromPartyId", partyIdValue(fromPartyId));
                result.setVariable("fromRole", DIGIPOORT_ROLE);
                result.setVariable("toPartyId", partyIdValue(OVERHEID_PARTY_ID));
                result.setVariable("toRole", OVERHEID_ROLE);
                result.setVariable("serviceType", "urn:osb:services");
                result.setVariable("service", "osb:afleveren:1.1$1.0");
                result.setVariable("action", ACTION);
                return result;
        }

        private static String partyIdValue(String partyId)
        {
                return partyId.substring(PARTY_TYPE.length() + 1);
        }

        private static String randomMessageId()
        {
                return UUID.randomUUID().toString();
        }
}
