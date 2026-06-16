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
package nl.clockwork.ebms.server.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.api.DeliveryManager;
import nl.clockwork.ebms.client.api.DeliveryTaskManager;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.dao.EbMSDAO;
import nl.clockwork.ebms.common.event.MessageEventListener;
import nl.clockwork.ebms.common.message.EbMSMessageFactory;
import nl.clockwork.ebms.common.security.EbMSSignatureGenerator;
import nl.clockwork.ebms.server.processing.EbMSMessageProcessor;
import nl.clockwork.ebms.server.processing.duplicate.DuplicateMessageHandler;
import nl.clockwork.ebms.server.validation.EbMSMessageValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSMessageProcessorConfig
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
