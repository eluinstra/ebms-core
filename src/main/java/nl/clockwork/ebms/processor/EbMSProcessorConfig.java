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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.EbMSMessageValidator;

@Configuration(proxyBeanMethods = false)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSProcessorConfig
{
	@Autowired
	DeliveryManager deliveryManager;
	@Autowired
	EventListener eventListener;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	CPAManager cpaManager;
	@Autowired
	EbMSMessageFactory ebMSMessageFactory;
	@Autowired
	EventManager eventManager;
	@Autowired
	EbMSSignatureGenerator signatureGenerator;
	@Autowired
	EbMSMessageValidator messageValidator;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@Value("${ebmsMessage.storeDuplicate}")
	boolean storeDuplicateMessage;
	@Value("${ebmsMessage.storeDuplicateContent}")
	boolean storeDuplicateMessageAttachments;
	
	@Bean
	public EbMSMessageProcessor messageProcessor()
	{
		val duplicateMessageHandler = DuplicateMessageHandler.builder()
				.setEbMSDAO(ebMSDAO)
				.setCpaManager(cpaManager)
				.setEventManager(eventManager)
				.setMessageValidator(messageValidator)
				.setStoreDuplicateMessage(storeDuplicateMessage)
				.setStoreDuplicateMessageAttachments(storeDuplicateMessageAttachments)
				.build();
		return EbMSMessageProcessor.builder()
				.setDeliveryManager(deliveryManager)
				.setEventListener(eventListener)
				.setEbMSDAO(ebMSDAO)
				.setCpaManager(cpaManager)
				.setEbMSMessageFactory(ebMSMessageFactory)
				.setEventManager(eventManager)
				.setSignatureGenerator(signatureGenerator)
				.setMessageValidator(messageValidator)
				.setDuplicateMessageHandler(duplicateMessageHandler)
				.setDeleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}
}
