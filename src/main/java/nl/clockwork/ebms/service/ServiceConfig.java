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
import nl.clockwork.ebms.cpa.certificate.CertificateMapper;
import nl.clockwork.ebms.cpa.url.URLMapper;
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
import nl.clockwork.ebms.validation.CPAValidator;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceConfig
{
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
		return EbMSMessageServiceImpl.builder()
				.setDeliveryManager(deliveryManager)
				.setEbMSDAO(ebMSDAO)
				.setEbMSMessageEventDAO(ebMSMessageEventDAO)
				.setCpaManager(cpaManager)
				.setEbMSMessageFactory(ebMSMessageFactory)
				.setEventManager(eventManager)
				.setEbMSMessageContextValidator(ebMSMessageContextValidator)
				.setSignatureGenerator(signatureGenerator)
				.setDeleteEbMSAttachmentsOnMessageProcessed(deleteEbMSAttachmentsOnMessageProcessed)
				.build();
	}

	@Bean
	public EbMSMessageServiceMTOM ebMSMessageServiceMTOM()
	{
		return new EbMSMessageServiceMTOMImpl(ebMSMessageService());
	}
}
