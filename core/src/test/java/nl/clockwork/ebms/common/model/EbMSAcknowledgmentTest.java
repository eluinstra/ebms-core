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
package nl.clockwork.ebms.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import lombok.val;
import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActorType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.From;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To;
import org.w3._2000._09.xmldsig.SignatureType;

class EbMSAcknowledgmentTest
{
	@Test
	void testEbMSAcknowledgmentBuilder()
	{
		val messageHeader = createMessageHeader();
		val acknowledgment = createAcknowledgment();

		val message = EbMSAcknowledgment.builder().messageHeader(messageHeader).acknowledgment(acknowledgment).build();

		assertThat(message.getMessageHeader()).isEqualTo(messageHeader);
		assertThat(message.getAcknowledgment()).isNotNull();
	}

	@Test
	void testEbMSAcknowledgmentWithSignature()
	{
		val messageHeader = createMessageHeader();
		val acknowledgment = createAcknowledgment();
		val signature = new SignatureType();

		val message = EbMSAcknowledgment.builder().messageHeader(messageHeader).signature(signature).acknowledgment(acknowledgment).build();

		assertThat(message.getSignature()).isEqualTo(signature);
	}

	@Test
	void testEbMSAcknowledgmentWithNullMessageHeader()
	{
		assertThatThrownBy(() -> EbMSAcknowledgment.builder().messageHeader(null).acknowledgment(createAcknowledgment()).build())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testEbMSAcknowledgmentWithNullAcknowledgment()
	{
		val messageHeader = createMessageHeader();
		assertThatThrownBy(() -> EbMSAcknowledgment.builder().messageHeader(messageHeader).acknowledgment(null).build())
				.isInstanceOf(IllegalArgumentException.class);
	}

	private MessageHeader createMessageHeader()
	{
		MessageData messageData = new MessageData();
		messageData.setMessageId("test-message-id");
		messageData.setTimestamp(java.time.Instant.now());

		PartyId partyId = new PartyId();
		partyId.setType("urn:osb:oin");
		partyId.setValue("00000000000000000001");

		Service service = new Service();
		service.setType("urn:osb:services");
		service.setValue("osb:aanleveren:1.1$1.0");

		MessageHeader messageHeader = new MessageHeader();
		messageHeader.setMessageData(messageData);
		messageHeader.setFrom(createFrom());
		messageHeader.setTo(createTo());
		messageHeader.setService(service);
		messageHeader.setAction("Acknowledgment");

		return messageHeader;
	}

	private From createFrom()
	{
		From from = new From();
		PartyId partyId = new PartyId();
		partyId.setType("urn:osb:oin");
		partyId.setValue("00000000000000000001");
		from.getPartyId().add(partyId);
		from.setRole("SENDER");
		return from;
	}

	private To createTo()
	{
		To to = new To();
		PartyId partyId = new PartyId();
		partyId.setType("urn:osb:oin");
		partyId.setValue("00000000000000000001");
		to.getPartyId().add(partyId);
		to.setRole("RECEIVER");
		return to;
	}

	private Acknowledgment createAcknowledgment()
	{
		Acknowledgment acknowledgment = new Acknowledgment();
		acknowledgment.setVersion("2.0");
		acknowledgment.setMustUnderstand(true);
		acknowledgment.setTimestamp(java.time.Instant.now());
		acknowledgment.setRefToMessageId("test-message-id");
		acknowledgment.setFrom(createFrom());
		acknowledgment.setActor(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_TO_PARTY_MSH.value());
		return acknowledgment;
	}
}
