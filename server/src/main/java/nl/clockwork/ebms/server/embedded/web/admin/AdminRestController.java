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
package nl.clockwork.ebms.server.embedded.web.admin;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.api.WithController;
import nl.clockwork.ebms.api.ebms.NotFoundException;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.server.embedded.dao.EbMSDAO;
import nl.clockwork.ebms.server.embedded.model.CPA;
import nl.clockwork.ebms.server.embedded.model.EbMSAttachment;
import nl.clockwork.ebms.server.embedded.model.EbMSMessage;
import nl.clockwork.ebms.server.embedded.web.message.TimeUnit;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminRestController implements WithController
{
	static final int DEFAULT_PAGE_SIZE = 50;
	static final int MAX_PAGE_SIZE = 500;

	@NonNull
	EbMSDAO ebMSDAO;

	@GET
	@Path("cpas")
	public List<CPA> getCPAs(@QueryParam("first") @DefaultValue("0") long first, @QueryParam("count") @DefaultValue("50") int count)
	{
		try
		{
			return ebMSDAO.selectCPAs(Math.max(first, 0), capPageSize(count));
		}
		catch (RuntimeException e)
		{
			log.error("GetCPAs", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("cpas/ids")
	public List<String> getCPAIds()
	{
		try
		{
			return ebMSDAO.selectCPAIds();
		}
		catch (RuntimeException e)
		{
			log.error("GetCPAIds", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("cpas/count")
	public long countCPAs()
	{
		try
		{
			return ebMSDAO.countCPAs();
		}
		catch (RuntimeException e)
		{
			log.error("CountCPAs", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("cpas/{cpaId}")
	public CPA getCPA(@PathParam("cpaId") String cpaId)
	{
		try
		{
			val cpa = ebMSDAO.findCPA(cpaId);
			if (cpa == null)
				throw new NotFoundException("CPA not found: " + cpaId);
			return cpa;
		}
		catch (RuntimeException e)
		{
			log.error("GetCPA {}", cpaId, e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages")
	public
			List<EbMSMessage>
			getMessages(@BeanParam MessageFilterQuery filter, @QueryParam("first") @DefaultValue("0") long first, @QueryParam("count") @DefaultValue("50") int count)
	{
		try
		{
			return ebMSDAO.selectMessages(filter.toFilter(), Math.max(first, 0), capPageSize(count));
		}
		catch (RuntimeException e)
		{
			log.error("GetMessages", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/count")
	public long countMessages(@BeanParam MessageFilterQuery filter)
	{
		try
		{
			return ebMSDAO.countMessages(filter.toFilter());
		}
		catch (RuntimeException e)
		{
			log.error("CountMessages", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/ids")
	public List<String> getMessageIds(
			@QueryParam("cpaId") String cpaId,
			@QueryParam("fromRole") String fromRole,
			@QueryParam("toRole") String toRole,
			@QueryParam("status") List<EbMSMessageStatus> statuses)
	{
		try
		{
			val statusArray = statuses == null ? new EbMSMessageStatus[0] : statuses.toArray(new EbMSMessageStatus[0]);
			return ebMSDAO.selectMessageIds(cpaId, fromRole, toRole, statusArray);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessageIds", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/traffic")
	public Map<Integer, Integer> getMessageTraffic(
			@QueryParam("from") LocalDateTime from,
			@QueryParam("to") LocalDateTime to,
			@QueryParam("unit") @DefaultValue("DAY") TimeUnit unit,
			@QueryParam("status") List<EbMSMessageStatus> statuses)
	{
		try
		{
			val statusArray = statuses == null ? new EbMSMessageStatus[0] : statuses.toArray(new EbMSMessageStatus[0]);
			return ebMSDAO.selectMessageTraffic(from, to, unit, statusArray);
		}
		catch (RuntimeException e)
		{
			log.error("GetMessageTraffic", e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/export.csv")
	@Produces("text/csv")
	public Response exportMessagesCsv(@BeanParam MessageFilterQuery filter)
	{
		val ebmsFilter = filter.toFilter();
		StreamingOutput output = stream ->
		{
			try (val writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8); val printer = new CSVPrinter(writer, CSVFormat.DEFAULT))
			{
				ebMSDAO.printMessagesToCSV(printer, ebmsFilter);
			}
		};
		return Response.ok(output, "text/csv").header("Content-Disposition", "attachment; filename=\"messages.csv\"").build();
	}

	@GET
	@Path("messages/{messageId}")
	public EbMSMessage getMessage(@PathParam("messageId") String messageId)
	{
		try
		{
			val message = ebMSDAO.findMessage(messageId);
			if (message == null)
				throw new NotFoundException("Message not found: " + messageId);
			return message;
		}
		catch (RuntimeException e)
		{
			log.error("GetMessage {}", messageId, e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/{messageId}/response")
	public EbMSMessage getResponseMessage(@PathParam("messageId") String messageId)
	{
		try
		{
			if (!ebMSDAO.existsResponseMessage(messageId))
				throw new NotFoundException("Response message not found for: " + messageId);
			return ebMSDAO.findResponseMessage(messageId);
		}
		catch (RuntimeException e)
		{
			log.error("GetResponseMessage {}", messageId, e);
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/{messageId}/export")
	@Produces("application/zip")
	public Response exportMessage(@PathParam("messageId") String messageId)
	{
		try
		{
			if (ebMSDAO.findMessage(messageId) == null)
				throw new NotFoundException("Message not found: " + messageId);
		}
		catch (RuntimeException e)
		{
			log.error("ExportMessage {}", messageId, e);
			throw toWebApplicationException(e);
		}
		StreamingOutput output = stream ->
		{
			try (val zip = new ZipOutputStream(stream))
			{
				ebMSDAO.writeMessageToZip(messageId, zip);
			}
		};
		return Response.ok(output, "application/zip").header("Content-Disposition", "attachment; filename=\"" + messageId + ".zip\"").build();
	}

	@GET
	@Path("messages/{messageId}/attachments/{contentId}")
	@Produces(MediaType.WILDCARD)
	public Response getAttachment(@PathParam("messageId") String messageId, @PathParam("contentId") String contentId)
	{
		EbMSAttachment attachment;
		try
		{
			attachment = ebMSDAO.findAttachment(messageId, contentId);
		}
		catch (RuntimeException e)
		{
			log.error("GetAttachment {} {}", messageId, contentId, e);
			throw toWebApplicationException(e);
		}
		if (attachment == null || attachment.getContent() == null)
			throw toWebApplicationException(new NotFoundException("Attachment not found: " + messageId + "/" + contentId));
		val content = attachment.getContent();
		StreamingOutput output = stream ->
		{
			try (val in = content.getInputStream())
			{
				in.transferTo(stream);
			}
		};
		val response = Response.ok(output, attachment.getContentType());
		if (attachment.getName() != null)
			response.header("Content-Disposition", "attachment; filename=\"" + attachment.getName() + "\"");
		return response.build();
	}

	private static int capPageSize(int count)
	{
		if (count <= 0)
			return DEFAULT_PAGE_SIZE;
		return Math.min(count, MAX_PAGE_SIZE);
	}
}
