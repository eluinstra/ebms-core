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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;
import nl.clockwork.ebms.service.model.MessageStatus;

@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@AllArgsConstructor
class EbMSMessageServiceImpl implements EbMSMessageService
{
	@NonNull
	EbMSMessageServiceHandler serviceHandler;

	@Override
	public void ping(String cpaId, String fromPartyId, String toPartyId) throws EbMSMessageServiceException
	{
		serviceHandler.ping(cpaId,fromPartyId,toPartyId);
	}

	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		return serviceHandler.sendMessage(messageContent);
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		return serviceHandler.resendMessage(messageId);
	}

	@Override
	public List<String> getUnprocessedMessageIds(EbMSMessageContext messageContext, Integer maxNr) throws EbMSMessageServiceException
	{
		return serviceHandler.getUnprocessedMessageIds(messageContext,maxNr);
	}

	@Override
	public EbMSMessageContent getMessage(final String messageId, Boolean process) throws EbMSMessageServiceException
	{
		return serviceHandler.getMessage(messageId,process);
	}

	@Override
	public void processMessage(final String messageId) throws EbMSMessageServiceException
	{
		serviceHandler.processMessage(messageId);
	}

	@Override
	public MessageStatus getMessageStatus(String messageId) throws EbMSMessageServiceException
	{
		return serviceHandler.getMessageStatus(messageId);
	}

	@Override
	public List<EbMSMessageEvent> getUnprocessedMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] eventTypes, Integer maxNr) throws EbMSMessageServiceException
	{
		return serviceHandler.getUnprocessedMessageEvents(messageContext,eventTypes,maxNr);
	}

	@Override
	public void processMessageEvent(final String messageId) throws EbMSMessageServiceException
	{
		serviceHandler.processMessageEvent(messageId);
	}
}
