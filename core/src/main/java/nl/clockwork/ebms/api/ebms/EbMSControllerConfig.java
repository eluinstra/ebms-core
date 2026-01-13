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
package nl.clockwork.ebms.api.ebms;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.api.cpa.CPAQueryManager;
import nl.clockwork.ebms.client.delivery.DeliveryManager;
import nl.clockwork.ebms.client.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.event.MessageEventDAO;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.MessagePropertiesValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSControllerConfig
{
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Bean
	public EbMSControllerImpl ebMSController(EbMSControllerHandler ebMSControllerHandler)
	{
		return new EbMSControllerImpl(ebMSControllerHandler);
	}

	@Bean
	public EbMSControllerMTOM ebMSControllerMTOM(EbMSControllerHandler ebMSControllerHandler)
	{
		return new EbMSControllerMTOMImpl(ebMSControllerHandler);
	}

	@Bean
	public EbMSRestController ebMSMessageRestService(EbMSControllerHandler ebMSControllerHandler)
	{
		return new EbMSRestController(ebMSControllerHandler);
	}

	@Bean
	public EbMSControllerHandler ebMSControllerHandler(
			DeliveryManager deliveryManager,
			EbMSDAO ebMSDAO,
			MessageEventDAO messageEventDAO,
			CPAQueryManager cpaManager,
			EbMSMessageFactory ebMSMessageFactory,
			DeliveryTaskManager deliveryTaskManager,
			MessagePropertiesValidator messagePropertiesValidator,
			EbMSSignatureGenerator signatureGenerator)
	{
		return EbMSControllerHandler.builder()
				.deliveryManager(deliveryManager)
				.ebMSDAO(ebMSDAO)
				.messageEventDAO(messageEventDAO)
				.cpaManager(cpaManager)
				.ebMSMessageFactory(ebMSMessageFactory)
				.deliveryTaskManager(deliveryTaskManager)
				.messagePropertiesValidator(messagePropertiesValidator)
				.signatureGenerator(signatureGenerator)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}
}
