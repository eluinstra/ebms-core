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
package nl.clockwork.ebms.service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.jaxrs.WithService;
import nl.clockwork.ebms.service.model.MTOMDataSource;
import nl.clockwork.ebms.service.model.MTOMMessageRequest;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.service.model.MessageRequestProperties;
import nl.clockwork.ebms.service.model.MessageStatus;

@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
@Path("ebms")
@Produces(MediaType.APPLICATION_JSON)
public class EbMSMessageServiceImpl implements EbMSMessageService, WithService
{
	@NonNull
	EbMSMessageServiceHandler serviceHandler;

	@POST
	@Path("ping/{cpaId}/from/{fromPartyId}/to/{toPartyId}")
	@Override
	public void ping(@PathParam("cpaId") String cpaId, @PathParam("fromPartyId") String fromPartyId, @PathParam("toPartyId") String toPartyId) throws EbMSMessageServiceException
	{
		try
		{
			serviceHandler.ping(cpaId,fromPartyId,toPartyId);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
		}
	}

	@POST
	@Path("messages")
	@Produces("text/plain")
	@Override
	public String sendMessage(MessageRequest messageRequest) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.sendMessage(messageRequest);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
			return null;
		}
	}

	@POST
	@Path("messages/mtom")
	@Produces("text/plain")
	@Consumes("multipart/form-data")
	public String sendMessage(@Multipart("requestProperties") MessageRequestProperties requestProperties, @Multipart("attachment") List<Attachment> attachments) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.sendMessageMTOM(new MTOMMessageRequest(requestProperties,attachments.stream().map(toMTOMDataSource()).collect(Collectors.toList())));
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
			return null;
		}
	}

	private Function<Attachment,MTOMDataSource> toMTOMDataSource()
	{
		return attachment -> new MTOMDataSource(attachment.getContentId(),attachment.getDataHandler());
	}

	@PUT
	@Path("messages/{messageId}")
	@Produces("text/plain")
	@Override
	public String resendMessage(@PathParam("messageId") String messageId) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.resendMessage(messageId);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
			return null;
		}
	}

	@POST
	@Path("messages/{maxNr}")
	@Override
	public List<String> getUnprocessedMessageIds(MessageFilter messageFilter, @PathParam("maxNr") Integer maxNr) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getUnprocessedMessageIds(messageFilter,maxNr);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
			return null;
		}
	}

	@GET
	@Path("messages/{messageId}/{process}")
	@Override
	public Message getMessage(@PathParam("messageId") final String messageId, @PathParam("process") Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			return serviceHandler.getMessage(messageId,process);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
			return null;
		}
	}

	@PATCH
	@Path("messages/{messageId}")
	@Override
	public void processMessage(final String messageId) throws EbMSMessageServiceException
	{
		try
		{
			serviceHandler.processMessage(messageId);
		}
		catch(EbMSMessageServiceException e)
		{
			throwServiceException(e);
		}
	}

	@GET
	@Path("messages/{messageId}/status")
	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		return serviceHandler.getMessageStatus(messageId);
	}

	@POST
	@Path("events/{eventTypes}/{maxNr}")
	@Override
	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, @PathParam("eventTypes") MessageEventType[] eventTypes, @PathParam("maxNr") Integer maxNr) throws EbMSMessageServiceException
	{
		return serviceHandler.getUnprocessedMessageEvents(messageFilter,eventTypes,maxNr);
	}

	@PATCH
	@Path("events/{messageId}")
	@Override
	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		serviceHandler.processMessageEvent(messageId);
	}
}
