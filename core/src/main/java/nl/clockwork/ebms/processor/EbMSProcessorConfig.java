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
package nl.clockwork.ebms.processor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.DeliveryManager;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSProcessorConfig
{
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Bean
	public EbMSMessageProcessor messageProcessor(
			DeliveryTaskManager deliveryTaskManager,
			MessageEventListener messageEventListener,
			EbMSDAO ebMSDAO,
			CPAManager cpaManager,
			EbMSMessageFactory ebMSMessageFactory,
			DeliveryManager deliveryManager,
			EbMSSignatureGenerator signatureGenerator,
			EbMSMessageValidator messageValidator)
	{
		val duplicateMessageHandler = DuplicateMessageHandler.builder()
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.deliveryTaskManager(deliveryTaskManager)
				.messageValidator(messageValidator)
				.build();
		return EbMSMessageProcessor.builder()
				.deliveryManager(deliveryManager)
				.messageEventListener(messageEventListener)
				.ebMSDAO(ebMSDAO)
				.cpaManager(cpaManager)
				.ebMSMessageFactory(ebMSMessageFactory)
				.deliveryTaskManager(deliveryTaskManager)
				.signatureGenerator(signatureGenerator)
				.messageValidator(messageValidator)
				.duplicateMessageHandler(duplicateMessageHandler)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}
}
