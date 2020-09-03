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
package nl.clockwork.ebms.processor;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.delivery.DeliveryManager;
import nl.clockwork.ebms.model.EbMSPing;
import nl.clockwork.ebms.model.EbMSPong;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidatorException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class PongProcessor
{
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
  DeliveryManager deliveryManager;

  public EbMSPong createPong(EbMSPing message, Instant timestamp) throws ValidatorException, EbMSProcessorException
	{
		return ebMSMessageFactory.createEbMSPong(message);
	}

  public void sendPong(final nl.clockwork.ebms.model.EbMSPong pong)
	{
		val responseMessageHeader = pong.getMessageHeader();
		val uri = cpaManager.getUri(
				responseMessageHeader.getCPAId(),
				responseMessageHeader.getTo().getPartyId(),
				responseMessageHeader.getTo().getRole(),
				CPAUtils.toString(responseMessageHeader.getService()),
				responseMessageHeader.getAction());
		deliveryManager.sendResponseMessage(uri,pong);
	}

  public void processPong(Instant timestamp, EbMSPong pong)
	{
		try
		{
			messageValidator.validate(pong,timestamp);
			deliveryManager.handleResponseMessage(pong);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process Pong " + pong.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}
}
