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


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.DeliveryManager;
import nl.clockwork.ebms.delivery.task.DeliveryTaskManager;
import nl.clockwork.ebms.event.MessageEventDAO;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.MessagePropertiesValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EbMSMessageServiceConfig
{
	@Autowired
	CPAManager cpaManager;
	@Autowired
	DeliveryManager deliveryManager;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	MessageEventDAO messageEventDAO;
	@Autowired
	EbMSMessageFactory ebMSMessageFactory;
	@Autowired
	DeliveryTaskManager deliveryTaskManager;
	@Autowired
	MessagePropertiesValidator messagePropertiesValidator;
	@Autowired
	EbMSSignatureGenerator signatureGenerator;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Bean
	public EbMSMessageServiceImpl ebMSMessageService()
	{
		return new EbMSMessageServiceImpl(ebMSMessageServiceHandler());
	}

	@Bean
	public EbMSMessageServiceMTOM ebMSMessageServiceMTOM()
	{
		return new EbMSMessageServiceMTOMImpl(ebMSMessageServiceHandler());
	}

	@Bean
	public EbMSMessageServiceHandler ebMSMessageServiceHandler()
	{
		return EbMSMessageServiceHandler.builder()
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
