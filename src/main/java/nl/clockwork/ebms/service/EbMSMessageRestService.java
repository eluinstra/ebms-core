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


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.jaxrs.WithService;
import nl.clockwork.ebms.service.model.MTOMDataSource;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MTOMMessageRequest;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageProperties;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import nl.clockwork.ebms.service.model.MessageStatus;
import nl.clockwork.ebms.service.model.Party;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EbMSMessageRestService implements WithService
{
	@NonNull
	EbMSMessageServiceHandler serviceHandler;

	@POST
	@Path("ping/{cpaId}/from/{fromPartyId}/to/{toPartyId}")
	public void ping(@PathParam("cpaId") String cpaId, @PathParam("fromPartyId") String fromPartyId, @PathParam("toPartyId") String toPartyId)
	{
		try
		{
			serviceHandler.ping(cpaId, fromPartyId, toPartyId);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}

	@POST
	@Path("messages")
	@Produces(MediaType.TEXT_PLAIN)
	public String sendMessage(MessageRequest messageRequest)
	{
		try
		{
			return serviceHandler.sendMessage(messageRequest);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@POST
	@Path("messages/mtom")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public String sendMessage(@Multipart("requestProperties") MessageRequestProperties requestProperties, @Multipart("attachment") List<Attachment> attachments)
	{
		try
		{
			return serviceHandler
					.sendMessageMTOM(new MTOMMessageRequest(requestProperties, attachments.stream().map(toMTOMDataSource()).collect(Collectors.toList())));
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	private Function<Attachment, MTOMDataSource> toMTOMDataSource()
	{
		return attachment -> new MTOMDataSource(attachment.getContentId(), attachment.getDataHandler());
	}

	@PUT
	@Path("messages/{messageId}")
	@Produces(MediaType.TEXT_PLAIN)
	public String resendMessage(@PathParam("messageId") String messageId)
	{
		try
		{
			return serviceHandler.resendMessage(messageId);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@GET
	@Path("messages/unprocessed")
	public List<String> getUnprocessedMessageIds(
			@QueryParam("cpaId") String cpaId,
			@QueryParam("fromPartyId") String fromPartyId,
			@QueryParam("fromRole") String fromRole,
			@QueryParam("toPartyId") String toPartyId,
			@QueryParam("toRole") String toRole,
			@QueryParam("service") String service,
			@QueryParam("action") String action,
			@QueryParam("conversationId") String conversationId,
			@QueryParam("messageId") String messageId,
			@QueryParam("refToMessageId") String refToMessageId,
			@QueryParam("maxNr") @DefaultValue("0") Integer maxNr)
	{
		try
		{
			return serviceHandler.getUnprocessedMessageIds(
					MessageFilter.builder()
							.cpaId(cpaId)
							.fromParty(fromPartyId == null ? null : new Party(fromPartyId, fromRole))
							.toParty(toPartyId == null ? null : new Party(toPartyId, toRole))
							.service(service)
							.action(action)
							.conversationId(conversationId)
							.messageId(messageId)
							.refToMessageId(refToMessageId)
							.build(),
					maxNr);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e, MediaType.TEXT_PLAIN);
		}
	}

	@GET
	@Path("messages/{messageId}")
	public Message getMessage(@PathParam("messageId") final String messageId, @QueryParam("process") @DefaultValue("false") Boolean process)

	{
		try
		{
			return serviceHandler.getMessage(messageId, process);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/mtom/{messageId}")
	@Produces(MediaType.MULTIPART_FORM_DATA)
	public MultipartBody getMessageRest(@PathParam("messageId") final String messageId, @QueryParam("process") @DefaultValue("false") Boolean process)

	{
		try
		{
			return toMultipartBody(serviceHandler.getMessageMTOM(messageId, process));
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e, MediaType.MULTIPART_FORM_DATA);
		}
	}

	private MultipartBody toMultipartBody(MTOMMessage message)
	{
		val attachments = new LinkedList<Attachment>();
		attachments.add(toAttachment(message.getProperties()));
		attachments.addAll(message.getDataSources().stream().map(this::toAttachment).collect(Collectors.toList()));
		return new MultipartBody(attachments, true);
	}

	private Attachment toAttachment(MessageProperties messageProperties)
	{
		return new Attachment("properties", "application/json", messageProperties);
	}

	private Attachment toAttachment(MTOMDataSource dataSource)
	{
		return new Attachment(dataSource.getContentId(), dataSource.getAttachment(), new MultivaluedHashMap<>());
	}

	@PATCH
	@Path("messages/{messageId}")
	public void processMessage(@PathParam("messageId") final String messageId)
	{
		try
		{
			serviceHandler.processMessage(messageId);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("messages/{messageId}/status")
	public MessageStatus getMessageStatus(@PathParam("messageId") String messageId)
	{
		try
		{
			return serviceHandler.getMessageStatus(messageId);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}

	@GET
	@Path("events/unprocessed")
	public List<MessageEvent> getUnprocessedMessageEvents(
			@QueryParam("cpaId") String cpaId,
			@QueryParam("fromPartyId") String fromPartyId,
			@QueryParam("fromRole") String fromRole,
			@QueryParam("toPartyId") String toPartyId,
			@QueryParam("toRole") String toRole,
			@QueryParam("service") String service,
			@QueryParam("action") String action,
			@QueryParam("conversationId") String conversationId,
			@QueryParam("messageId") String messageId,
			@QueryParam("refToMessageId") String refToMessageId,
			@QueryParam("eventTypes") MessageEventType[] eventTypes,
			@QueryParam("maxNr") @DefaultValue("0") Integer maxNr)
	{
		try
		{
			return serviceHandler.getUnprocessedMessageEvents(
					MessageFilter.builder()
							.cpaId(cpaId)
							.fromParty(fromPartyId == null ? null : new Party(fromPartyId, fromRole))
							.toParty(toPartyId == null ? null : new Party(toPartyId, toRole))
							.service(service)
							.action(action)
							.conversationId(conversationId)
							.messageId(messageId)
							.refToMessageId(refToMessageId)
							.build(),
					eventTypes,
					maxNr);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}

	@PATCH
	@Path("events/{messageId}")
	public void processMessageEvent(@PathParam("messageId") final String messageId)
	{
		try
		{
			serviceHandler.processMessageEvent(messageId);
		}
		catch (Exception e)
		{
			throw toWebApplicationException(e);
		}
	}
}
