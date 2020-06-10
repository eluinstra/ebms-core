package nl.clockwork.ebms.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.AccessLevel;
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
		DuplicateMessageHandler duplicateMessageHandler = DuplicateMessageHandler.builder()
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
