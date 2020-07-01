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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CertificateMapper;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.service.cpa.CPAService;
import nl.clockwork.ebms.service.cpa.CPAServiceImpl;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMappingService;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMappingServiceImpl;
import nl.clockwork.ebms.service.cpa.url.URLMappingService;
import nl.clockwork.ebms.service.cpa.url.URLMappingServiceImpl;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.transaction.TransactionTemplate;
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceConfig
{
	@Autowired
	TransactionTemplate dataSourceTransactionTemplate;
	@Autowired
	CPAManager cpaManager;
	@Autowired
	CPAValidator cpaValidator;
	@Autowired
	URLMapper urlMapper;
	@Autowired
	CertificateMapper certificateMapper;
	@Autowired
	DeliveryManager deliveryManager;
	@Autowired
	EbMSDAO ebMSDAO;
	@Autowired
	EbMSMessageEventDAO ebMSMessageEventDAO;
	@Autowired
	EbMSMessageFactory ebMSMessageFactory;
	@Autowired
	EventManager eventManager;
	@Autowired
	EbMSMessageContextValidator ebMSMessageContextValidator;
	@Autowired
	EbMSSignatureGenerator signatureGenerator;
	@Value("${ebmsMessage.deleteContentOnProcessed}")
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	@Bean
	public CPAService cpaService()
	{
		return new CPAServiceImpl(cpaManager,cpaValidator);
	}

	@Bean
	public URLMappingService urlMappingService()
	{
		return new URLMappingServiceImpl(urlMapper);
	}

	@Bean
	public CertificateMappingService certificateMappingService()
	{
		return new CertificateMappingServiceImpl(certificateMapper);
	}

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
				.transactionTemplate(dataSourceTransactionTemplate)
				.deliveryManager(deliveryManager)
				.ebMSDAO(ebMSDAO)
				.ebMSMessageEventDAO(ebMSMessageEventDAO)
				.cpaManager(cpaManager)
				.ebMSMessageFactory(ebMSMessageFactory)
				.eventManager(eventManager)
				.ebMSMessageContextValidator(ebMSMessageContextValidator)
				.signatureGenerator(signatureGenerator)
				.deleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}
}
