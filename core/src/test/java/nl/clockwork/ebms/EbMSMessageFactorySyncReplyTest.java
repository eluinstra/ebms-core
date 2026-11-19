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
package nl.clockwork.ebms;

import static nl.clockwork.ebms.api.cpa.CPATestUtils.cpaCache;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.GeneralSecurityException;
import java.io.IOException;
import nl.clockwork.ebms.api.ebms.model.MessageRequest;
import nl.clockwork.ebms.api.ebms.model.MessageRequestProperties;
import nl.clockwork.ebms.api.ebms.model.Party;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.cpa.CPARepository;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.security.EbMSKeyStore;
import nl.clockwork.ebms.security.KeyStoreType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.Mockito;
import lombok.val;

/**
 * Verifies that the outgoing {@code SyncReply} header is derived from the delivery channel the message's
 * action is actually bound to ({@code canSend.thisPartyActionBinding}), not from the party's default MSH
 * channel ({@code defaultMshChannelId}).
 * <p>
 * The {@code cpaStubEBF.be.http.unsigned.dualchannel} fixture models a CPA where the two diverge: the
 * DIGIPOORT party's default channel is the best-effort channel ({@code syncReplyMode="none"}) while the
 * {@code afleveren} action is bound to a second channel with
 * {@code syncReplyMode="mshSignalsOnly"}/{@code ackRequested="always"} (the BBO scenario: a reliable
 * transport channel referenced by the action binding, a best-effort default channel).
 */
@TestInstance(value = Lifecycle.PER_CLASS)
public class EbMSMessageFactorySyncReplyTest
{
	private static final String CPA_ID = "cpaStubEBF.be.http.unsigned.dualchannel";
	private static final String DIGIPOORT_PARTY_ID = "urn:osb:oin:00000000000000000000";
	private static final String DIGIPOORT_ROLE = "DIGIPOORT";
	private static final String AFGLEVEREN_SERVICE = "urn:osb:services:osb:afleveren:1.1$1.0";
	private static final String AANLEVEREN_SERVICE = "urn:osb:services:osb:aanleveren:1.1$1.0";

	private EbMSMessageFactory messageFactory;

	@BeforeAll
	void init() throws GeneralSecurityException, IOException
	{
		val cpaRepository = Mockito.mock(CPARepository.class);
		Mockito.when(cpaRepository.getCPA(CPA_ID)).thenReturn(cpaCache.apply(CPA_ID));
		val cpaManager = new CPAManager(
				cpaRepository,
				(cpa, certificate) -> certificate,
				url -> url,
				EbMSKeyStore.of(KeyStoreType.PKCS12, "nl/clockwork/ebms/keystore.p12", "password", "password"),
				false);
		messageFactory = new EbMSMessageFactory(cpaManager, new EbMSIdGenerator());
	}

	/**
	 * The action is bound to the reliable channel (syncReplyMode=mshSignalsOnly, ackRequested=always) even
	 * though the party's default channel has syncReplyMode=none. The outgoing message must carry the
	 * SyncReply header and the AckRequested header derived from that same bound channel.
	 */
	@Test
	void syncReplyIsDerivedFromTheActionBoundChannel() throws Exception
	{
		val message = buildMessage(DIGIPOORT_PARTY_ID, DIGIPOORT_ROLE, AFGLEVEREN_SERVICE, "afleveren");

		assertThat(message.getSyncReply()).as("SyncReply").isNotNull();
		assertThat(message.getSyncReply().getActor()).isEqualTo(Constants.NSURI_SOAP_NEXT_ACTOR);
		assertThat(message.getSyncReply().isMustUnderstand()).isTrue();
		assertThat(message.getSyncReply().getVersion()).isEqualTo(Constants.EBMS_VERSION);
		// the bound channel has ackRequested=always / duplicateElimination=always: the header must be
		// consistent with it, i.e. derived from the same channel as the SyncReply
		assertThat(message.getAckRequested()).as("AckRequested").isNotNull();
		assertThat(message.getMessageHeader().getDuplicateElimination()).as("DuplicateElimination").isNotNull();
	}

	/**
	 * An action bound to the default (best-effort, syncReplyMode=none) channel must NOT carry a SyncReply
	 * header — regardless of what another channel of the same party defines.
	 */
	@Test
	void syncReplyIsAbsentWhenBoundToDefaultChannel() throws Exception
	{
		val message = buildMessage(DIGIPOORT_PARTY_ID, DIGIPOORT_ROLE, AANLEVEREN_SERVICE, "bevestigAanleveren");

		assertThat(message.getSyncReply()).as("SyncReply").isNull();
		assertThat(message.getAckRequested()).as("AckRequested").isNull();
		assertThat(message.getMessageHeader().getDuplicateElimination()).as("DuplicateElimination").isNull();
	}

	private EbMSMessage buildMessage(String fromPartyId, String fromRole, String service, String action) throws Exception
	{
		val properties = new MessageRequestProperties(CPA_ID, new Party(fromPartyId, fromRole), service, action);
		return messageFactory.createEbMSMessage(MessageRequest.builder().properties(properties).build());
	}
}
