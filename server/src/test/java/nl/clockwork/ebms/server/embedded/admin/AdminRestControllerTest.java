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
package nl.clockwork.ebms.server.embedded.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nl.clockwork.ebms.api.ebms.exception.NotFoundException;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.server.embedded.dao.EbMSDAO;
import nl.clockwork.ebms.server.message.model.core.CPA;
import nl.clockwork.ebms.server.message.model.core.EbMSAttachment;
import nl.clockwork.ebms.server.message.model.core.EbMSMessage;
import nl.clockwork.ebms.server.message.model.embedded.web.EbMSMessageFilter;
import nl.clockwork.ebms.server.message.model.embedded.web.MessageFilterQuery;
import org.apache.cxf.io.CachedOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AdminRestControllerTest
{
	@Mock
	EbMSDAO ebMSDAO;

	AutoCloseable mocks;
	AdminRestController controller;

	@BeforeEach
	void setUp()
	{
		mocks = MockitoAnnotations.openMocks(this);
		controller = new AdminRestController(ebMSDAO);
	}

	@AfterEach
	void tearDown() throws Exception
	{
		mocks.close();
	}

	@Test
	void getCPAIdsDelegates()
	{
		when(ebMSDAO.selectCPAIds()).thenReturn(List.of("cpa-1", "cpa-2"));
		assertThat(controller.getCPAIds()).containsExactly("cpa-1", "cpa-2");
	}

	@Test
	void countCPAsDelegates()
	{
		when(ebMSDAO.countCPAs()).thenReturn(7L);
		assertThat(controller.countCPAs()).isEqualTo(7L);
	}

	@Test
	void getCPAsAppliesDefaultPageSize()
	{
		when(ebMSDAO.selectCPAs(0L, AdminRestController.DEFAULT_PAGE_SIZE)).thenReturn(Collections.emptyList());
		controller.getCPAs(0L, 0);
		verify(ebMSDAO).selectCPAs(0L, AdminRestController.DEFAULT_PAGE_SIZE);
	}

	@Test
	void getCPAsCapsPageSize()
	{
		when(ebMSDAO.selectCPAs(eq(0L), eq(AdminRestController.MAX_PAGE_SIZE))).thenReturn(Collections.emptyList());
		controller.getCPAs(0L, 10_000);
		verify(ebMSDAO).selectCPAs(0L, AdminRestController.MAX_PAGE_SIZE);
	}

	@Test
	void getCPAReturnsResult()
	{
		var cpa = new CPA();
		when(ebMSDAO.findCPA("cpa-1")).thenReturn(cpa);
		assertThat(controller.getCPA("cpa-1")).isSameAs(cpa);
	}

	@Test
	void getCPAMissingYields404()
	{
		when(ebMSDAO.findCPA("missing")).thenReturn(null);
		assertThatThrownBy(() -> controller.getCPA("missing")).isInstanceOf(NotFoundException.class).hasMessageContaining("CPA not found: missing");
	}

	@Test
	void getMessageMissingYields404()
	{
		when(ebMSDAO.findMessage("missing")).thenReturn(null);
		assertThatThrownBy(() -> controller.getMessage("missing")).isInstanceOf(NotFoundException.class).hasMessageContaining("Message not found: missing");
	}

	@Test
	void getResponseMessageMissingYields404()
	{
		when(ebMSDAO.existsResponseMessage("missing")).thenReturn(false);
		assertThatThrownBy(() -> controller.getResponseMessage("missing")).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("Response message not found for: missing");
	}

	@Test
	void getMessagesPassesFilterAndDefaultsPageSize()
	{
		when(ebMSDAO.selectMessages(any(), eq(0L), eq((long)AdminRestController.DEFAULT_PAGE_SIZE))).thenReturn(Collections.emptyList());
		var query = new MessageFilterQuery();
		query.setCpaId("cpa-1");
		query.setFromPartyId("partyA");
		query.setFromRole("Sender");
		query.setStatuses(List.of(EbMSMessageStatus.RECEIVED));

		controller.getMessages(query, 0L, 0);

		var captor = ArgumentCaptor.forClass(EbMSMessageFilter.class);
		verify(ebMSDAO).selectMessages(captor.capture(), eq(0L), eq((long)AdminRestController.DEFAULT_PAGE_SIZE));
		var filter = captor.getValue();
		assertThat(filter.getCpaId()).isEqualTo("cpa-1");
		assertThat(filter.getFromParty()).isNotNull();
		assertThat(filter.getFromParty().getPartyId()).isEqualTo("partyA");
		assertThat(filter.getFromParty().getRole()).isEqualTo("Sender");
		assertThat(filter.getStatuses()).containsExactly(EbMSMessageStatus.RECEIVED);
	}

	@Test
	void getMessageIdsForwardsStatusArray()
	{
		when(ebMSDAO.selectMessageIds(eq("cpa-1"), eq("Sender"), eq("Receiver"), any(EbMSMessageStatus[].class))).thenReturn(List.of("m-1"));
		var result = controller.getMessageIds("cpa-1", "Sender", "Receiver", List.of(EbMSMessageStatus.RECEIVED, EbMSMessageStatus.PROCESSED));
		assertThat(result).containsExactly("m-1");
	}

	@Test
	void getMessageTrafficReturnsDaoResult()
	{
		when(ebMSDAO.selectMessageTraffic(any(), any(), any(), any(EbMSMessageStatus[].class))).thenReturn(Map.of(1, 5));
		assertThat(controller.getMessageTraffic(null, null, null, null)).containsEntry(1, 5);
	}

	@Test
	void getAttachmentStreamsContent() throws Exception
	{
		var payload = "hello".getBytes();
		var cached = new CachedOutputStream();
		cached.write(payload);
		cached.lockOutputStream();
		var attachment = new EbMSAttachment("file.txt", "cid-1", "text/plain", cached);
		when(ebMSDAO.findAttachment("m-1", "cid-1")).thenReturn(attachment);

		var response = controller.getAttachment("m-1", "cid-1");
		assertThat(response.getMediaType().toString()).isEqualTo("text/plain");
		assertThat(response.getHeaderString("Content-Disposition")).contains("file.txt");

		var output = (jakarta.ws.rs.core.StreamingOutput)response.getEntity();
		var buffer = new ByteArrayOutputStream();
		output.write(buffer);
		assertThat(buffer.toByteArray()).isEqualTo(payload);
	}

	@Test
	void getAttachmentMissingYields404()
	{
		when(ebMSDAO.findAttachment("m-1", "missing")).thenReturn(null);
		assertThatThrownBy(() -> controller.getAttachment("m-1", "missing")).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("Attachment not found: m-1/missing");
	}

	@Test
	void exportMessageMissingYields404()
	{
		when(ebMSDAO.findMessage("missing")).thenReturn(null);
		assertThatThrownBy(() -> controller.exportMessage("missing")).isInstanceOf(NotFoundException.class).hasMessageContaining("Message not found: missing");
	}

	@Test
	void exportMessageReturnsZipStream()
	{
		when(ebMSDAO.findMessage("m-1")).thenReturn(new EbMSMessage());
		var response = controller.exportMessage("m-1");
		assertThat(response.getMediaType().toString()).isEqualTo("application/zip");
		assertThat(response.getHeaderString("Content-Disposition")).contains("m-1.zip");
	}

	@Test
	void exportMessagesCsvReturnsCsvStream()
	{
		var response = controller.exportMessagesCsv(new MessageFilterQuery());
		assertThat(response.getMediaType().toString()).isEqualTo("text/csv");
		assertThat(response.getHeaderString("Content-Disposition")).contains("messages.csv");
	}
}
