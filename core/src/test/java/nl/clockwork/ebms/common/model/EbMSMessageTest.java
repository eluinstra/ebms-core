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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.From;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageData;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To;
import org.w3._2000._09.xmldsig.SignatureType;

class EbMSMessageTest
{
	@Test
	void testEbMSMessageBuilder()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();

		val message = EbMSMessage.builder().messageHeader(messageHeader).attachments(attachments).build();

		assertThat(message.getMessageHeader()).isEqualTo(messageHeader);
		assertThat(message.getAttachments()).isEmpty();
	}

	@Test
	void testEbMSMessageBuilderWithNullAttachments()
	{
		val messageHeader = createMessageHeader();

		val message = EbMSMessage.builder().messageHeader(messageHeader).attachments(null).build();

		assertThat(message.getAttachments()).isEmpty();
	}

	@Test
	void testEbMSMessageBuilderWithSingleAttachment() throws IOException
	{
		val messageHeader = createMessageHeader();
		val attachments = createAttachments(3);

		val message = EbMSMessage.builder().messageHeader(messageHeader).attachments(attachments).build();

		assertThat(message.getAttachments()).hasSize(3);
	}

	@Test
	void testGetContentId()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();

		val message = EbMSMessage.builder().messageHeader(messageHeader).attachments(attachments).build();

		assertThat(message.getContentId()).isEqualTo("test-message-id");
	}

	@Test
	void testEbMSMessageWithSignature()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();
		val signature = new SignatureType();

		val message = EbMSMessage.builder().messageHeader(messageHeader).signature(signature).attachments(attachments).build();

		assertThat(message.getSignature()).isEqualTo(signature);
	}

	@Test
	void testEbMSMessageWithSyncReply()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();
		val syncReply = new SyncReply();
		syncReply.setActor("test-actor");
		syncReply.setVersion("2.0");
		syncReply.setMustUnderstand(true);

		val message = EbMSMessage.builder().messageHeader(messageHeader).syncReply(syncReply).attachments(attachments).build();

		assertThat(message.getSyncReply()).isNotNull();
		assertThat(message.getSyncReply().getActor()).isEqualTo("test-actor");
	}

	@Test
	void testEbMSMessageWithMessageOrder()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();
		val messageOrder = new org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder();
		messageOrder.setSequenceNumber(new org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SequenceNumberType());
		messageOrder.setId("order-123");
		messageOrder.setVersion("2.0");
		messageOrder.setMustUnderstand(true);

		val message = EbMSMessage.builder().messageHeader(messageHeader).messageOrder(messageOrder).attachments(attachments).build();

		assertThat(message.getMessageOrder()).isNotNull();
	}

	@Test
	void testEbMSMessageWithAckRequested()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();
		val ackRequested = new AckRequested();
		ackRequested.setSigned(true);
		ackRequested.setActor("test-actor");
		ackRequested.setVersion("2.0");
		ackRequested.setMustUnderstand(true);

		val message = EbMSMessage.builder().messageHeader(messageHeader).ackRequested(ackRequested).attachments(attachments).build();

		assertThat(message.getAckRequested()).isNotNull();
		assertThat(message.getAckRequested().isSigned()).isTrue();
	}

	@Test
	void testEbMSMessageWithManifest()
	{
		val messageHeader = createMessageHeader();
		val attachments = new ArrayList<EbMSAttachment>();
		val manifest = new Manifest();
		manifest.setVersion("2.0");

		val message = EbMSMessage.builder().messageHeader(messageHeader).manifest(manifest).attachments(attachments).build();

		assertThat(message.getManifest()).isNotNull();
	}

	@Test
	void testEbMSMessageWithNullMessageHeader()
	{
		assertThatThrownBy(() -> EbMSMessage.builder().messageHeader(null).attachments(new ArrayList<EbMSAttachment>()).build())
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
		messageHeader.setAction("test-action");

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

	private List<EbMSAttachment> createAttachments(int count) throws IOException
	{
		val attachments = new ArrayList<EbMSAttachment>();
		for (int i = 0; i < count; i++)
		{
			val attachment = new PlainEbMSAttachment("attachment-" + i, new jakarta.mail.util.ByteArrayDataSource("Test content " + i, "text/plain"));
			attachments.add(attachment);
		}
		return attachments;
	}
}
