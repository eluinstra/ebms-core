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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.service.model.MessageRequest;
import nl.clockwork.ebms.event.MessageEventType;
import nl.clockwork.ebms.jaxrs.WithService;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;
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
	@Path("message/send")
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
	@Path("message/{messageId}/resend")
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
	@Path("message/unprocessed/{maxNr}")
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
	@Path("message/{messageId}/process/{process}")
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

	@POST
	@Path("message/{messageId}/process")
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
	@Path("message/{messageId}/status")
	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		return serviceHandler.getMessageStatus(messageId);
	}

	@POST
	@Path("message/event/{eventTypes}/{maxNr}")
	@Override
	public List<MessageEvent> getUnprocessedMessageEvents(MessageFilter messageFilter, @PathParam("eventTypes") MessageEventType[] eventTypes, @PathParam("maxNr") Integer maxNr) throws EbMSMessageServiceException
	{
		return serviceHandler.getUnprocessedMessageEvents(messageFilter,eventTypes,maxNr);
	}

	@POST
	@Path("message/{messageId}/event/process")
	@Override
	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		serviceHandler.processMessageEvent(messageId);
	}
}
