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
package nl.clockwork.ebms.server.processing.pong;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.client.api.DeliveryManager;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.message.EbMSMessageFactory;
import nl.clockwork.ebms.common.model.EbMSPing;
import nl.clockwork.ebms.common.model.EbMSPong;
import nl.clockwork.ebms.common.util.ValidatorException;
import nl.clockwork.ebms.server.processing.EbMSProcessorException;
import nl.clockwork.ebms.server.validation.EbMSMessageValidator;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class PongProcessor
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSMessageValidator messageValidator;
	@NonNull
	EbMSMessageFactory ebMSMessageFactory;
	@NonNull
	DeliveryManager deliveryManager;

	public EbMSPong createPong(EbMSPing message) throws ValidatorException, EbMSProcessorException
	{
		return ebMSMessageFactory.createEbMSPong(message);
	}

	public void sendPong(final nl.clockwork.ebms.common.model.EbMSPong pong)
	{
		val responseMessageHeader = pong.getMessageHeader();
		val uri = cpaManager.getReceivingUri(
				responseMessageHeader.getCPAId(),
				responseMessageHeader.getTo().getPartyId(),
				responseMessageHeader.getTo().getRole(),
				responseMessageHeader.getService(),
				responseMessageHeader.getAction());
		deliveryManager.sendResponseMessage(uri, pong);
	}

	public void processPong(EbMSPong pong)
	{
		try
		{
			messageValidator.validate(pong);
			deliveryManager.handleResponseMessage(pong);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process Pong " + pong.getMessageHeader().getMessageData().getMessageId(), e);
		}
	}
}
