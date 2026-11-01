/*
 * Copyright 2011 - 2026 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

import static nl.clockwork.ebms.api.cpa.CPATestUtils.cpaCache;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import lombok.val;
import nl.clockwork.ebms.api.ebms.model.MessageRequest;
import nl.clockwork.ebms.api.ebms.model.MessageRequestProperties;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPARepository;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.KeyStoreType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

/**
 * Verifies the party fields that the receive-side Acknowledgment and MessageError responses carry back for a CPA
 * whose party has multiple partyIds (the {@code cpaStubEBF.rm.https.signed.multi} fixture: DIGIPOORT is bound to
 * both {@code 00000000000000000000} and {@code 90000000000000000001}).
 * <p>
 * A sender picks one of the party's partyIds (here the "second" one) for the request's {@code from}; the responses
 * are built by reversing the request's parties, so the partyId the sender used must be echoed back as the response's
 * {@code to}. The Acknowledgment preserves the roles; the MessageError drops them (role is {@code null}) — both
 * behaviors are asserted here so the asymmetry is pinned down.
 * <p>
 * This exercises the exact factory methods the {@code AcknowledgmentProcessor} and {@code MessageErrorProcessor}
 * call, so it is a deterministic stand-in for the async response path.
 */
@TestInstance(value = Lifecycle.PER_CLASS)
public class EbMSMessageFactoryResponsePartyTest
{
	private static final String MULTI_CPA_ID = "cpaStubEBF.rm.https.signed.multi";
	private static final String PARTY_TYPE = "urn:osb:oin";
	private static final String DIGIPOORT_ROLE = "DIGIPOORT";
	private static final String OVERHEID_ROLE = "OVERHEID";
	private static final String DIGIPOORT_PARTY_ID = PARTY_TYPE + ":00000000000000000000";
	private static final String DIGIPOORT_PARTY_ID_SECOND = PARTY_TYPE + ":90000000000000000001";
	private static final String OVERHEID_PARTY_ID = PARTY_TYPE + ":00000000000000000001";
	private static final String SERVICE = "urn:osb:services:osb:afleveren:1.1$1.0";
	private static final String ACTION = "afleveren";

	private CPAManager cpaManager;
	private EbMSMessageFactory messageFactory;

	@BeforeAll
	void init() throws GeneralSecurityException, java.io.IOException
	{
		val cpaRepository = Mockito.mock(CPARepository.class);
		Mockito.when(cpaRepository.getCPA(MULTI_CPA_ID)).thenReturn(cpaCache.apply(MULTI_CPA_ID));
		cpaManager = new CPAManager(
				cpaRepository,
				(cpa, certificate) -> certificate,
				url -> url,
				EbMSKeyStore.of(KeyStoreType.PKCS12, "nl/clockwork/ebms/keystore.p12", "password", "password"),
				false);
		messageFactory = new EbMSMessageFactory(cpaManager, new EbMSIdGenerator());
	}

	static Stream<Arguments> partyIdProvider()
	{
		return Stream.of(Arguments.of(DIGIPOORT_PARTY_ID), Arguments.of(DIGIPOORT_PARTY_ID_SECOND));
	}

	/**
	 * The request must carry the single partyId the sender picked (not both of the party's partyIds).
	 */
	@ParameterizedTest
	@MethodSource("partyIdProvider")
	void requestFromCarriesTheSelectedPartyId(String senderPartyId) throws Exception
	{
		val message = buildRequest(senderPartyId);
		MessageHeader header = message.getMessageHeader();
		assertThat(header.getFrom().getPartyId()).extracting(Object::toString).containsExactly(senderPartyId);
		assertThat(header.getFrom().getRole()).isEqualTo(DIGIPOORT_ROLE);
		assertThat(header.getTo().getPartyId()).extracting(Object::toString).containsExactly(OVERHEID_PARTY_ID);
	}

	/**
	 * The Acknowledgment echoes the sender's partyId (as the response's {@code to}) and preserves the roles.
	 */
	@ParameterizedTest
	@MethodSource("partyIdProvider")
	void acknowledgmentEchoesSenderPartyIdAndPreservesRoles(String senderPartyId) throws Exception
	{
		val message = buildRequest(senderPartyId);
		val acknowledgment = messageFactory.createEbMSAcknowledgment(message, Instant.now());
		MessageHeader header = acknowledgment.getMessageHeader();

		// response to = request from = the DIGIPOORT partyId the sender selected
		assertThat(header.getTo().getPartyId()).extracting(Object::toString).containsExactly(senderPartyId);
		assertThat(header.getTo().getRole()).isEqualTo(DIGIPOORT_ROLE);
		// response from = request to = the OVERHEID party
		assertThat(header.getFrom().getPartyId()).extracting(Object::toString).containsExactly(OVERHEID_PARTY_ID);
		assertThat(header.getFrom().getRole()).isEqualTo(OVERHEID_ROLE);
		// the inner <Acknowledgment><From> is the acknowledging (receiving) party, i.e. the response header's from
		List<String> ackFrom = acknowledgment.getAcknowledgment().getFrom().getPartyId().stream()
				.map(Object::toString)
				.collect(java.util.stream.Collectors.toList());
		assertThat(ackFrom).containsExactly(OVERHEID_PARTY_ID);
		assertThat(acknowledgment.getAcknowledgment().getRefToMessageId()).isEqualTo(message.getMessageHeader().getMessageData().getMessageId());
	}

	/**
	 * The MessageError echoes the sender's partyId (as the response's {@code to}) but drops the roles.
	 */
	@ParameterizedTest
	@MethodSource("partyIdProvider")
	void messageErrorEchoesSenderPartyIdAndDropsRoles(String senderPartyId) throws Exception
	{
		val message = buildRequest(senderPartyId);
		val error = messageFactory.createEbMSMessageError(message, new ErrorList(), Instant.now());
		MessageHeader header = error.getMessageHeader();

		// response to = request from = the DIGIPOORT partyId the sender selected
		assertThat(header.getTo().getPartyId()).extracting(Object::toString).containsExactly(senderPartyId);
		assertThat(header.getTo().getRole()).isNull();
		// response from = request to = the OVERHEID party (role dropped too)
		assertThat(header.getFrom().getPartyId()).extracting(Object::toString).containsExactly(OVERHEID_PARTY_ID);
		assertThat(header.getFrom().getRole()).isNull();
		// a MessageError action must be used
		assertThat(header.getAction()).isEqualTo("MessageError");
	}

	private EbMSMessage buildRequest(String senderPartyId) throws Exception
	{
		val properties = new MessageRequestProperties(MULTI_CPA_ID, new Party(senderPartyId, DIGIPOORT_ROLE), SERVICE, ACTION);
		return messageFactory.createEbMSMessage(MessageRequest.builder().properties(properties).build());
	}
}
