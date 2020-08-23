package nl.clockwork.ebms.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.metrics.MetricsService;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EbMSMessageServiceMetricsHandler extends EbMSMessageServiceHandler
{
	@NonNull
	MetricsService metricsService;

	@Builder(builderMethodName = "ebMSMessageServiceMetricsHandlerBuilder")
	public EbMSMessageServiceMetricsHandler(@NonNull MetricsService metricsService, @NonNull DeliveryManager deliveryManager, @NonNull EbMSDAO ebMSDAO, @NonNull EbMSMessageEventDAO ebMSMessageEventDAO, @NonNull CPAManager cpaManager, @NonNull EbMSMessageFactory ebMSMessageFactory, @NonNull EventManager eventManager, @NonNull EbMSMessageContextValidator ebMSMessageContextValidator, @NonNull EbMSSignatureGenerator signatureGenerator, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		super(deliveryManager,ebMSDAO,ebMSMessageEventDAO,cpaManager,ebMSMessageFactory,eventManager,ebMSMessageContextValidator,signatureGenerator,deleteEbMSAttachmentsOnMessageProcessed);
		this.metricsService = metricsService;
	}

	@Override
	public String sendMessage(EbMSMessageContent messageContent) throws EbMSMessageServiceException
	{
		String result = super.sendMessage(messageContent);
		metricsService.increment("EbMSMessageService.sendMessage");
		return result;
	}

	@Override
	public String sendMessageMTOM(EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException
	{
		String result = super.sendMessageMTOM(messageContent);
		metricsService.increment("EbMSMessageService.sendMessage");
		return result;
	}

	@Override
	public String resendMessage(String messageId) throws EbMSMessageServiceException
	{
		String result = super.resendMessage(messageId);
		metricsService.increment("EbMSMessageService.resendMessage");
		return result;
	}

	@Override
	public void processMessage(String messageId) throws EbMSMessageServiceException
	{
		super.processMessage(messageId);
		metricsService.increment("EbMSMessageService.processMessage");
	}

	@Override
	public void processMessageEvent(String messageId) throws EbMSMessageServiceException
	{
		super.processMessageEvent(messageId);
		metricsService.increment("EbMSMessageService.processMessageEvent");
	}
}
