package nl.clockwork.ebms.event.processor;

import java.security.cert.CertificateException;
import java.time.Instant;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.client.EbMSClient;
import nl.clockwork.ebms.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.client.EbMSResponseException;
import nl.clockwork.ebms.client.EbMSUnrecoverableResponseException;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.URLMapper;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.encryption.EbMSMessageEncrypter;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSMessageProcessor;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@Builder(setterPrefix = "set")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class HandleEventTask implements Runnable
{
	@NonNull
	EventListener eventListener;
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	URLMapper urlMapper;
	@NonNull
	EventManager eventManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	EbMSMessageEncrypter messageEncrypter;
	@NonNull
	EbMSMessageProcessor messageProcessor;
	boolean deleteEbMSAttachmentsOnMessageProcessed;
	@NonNull
	EbMSEvent event;
	
	@Override
	public void run()
	{
		if (event.getTimeToLive() == null || Instant.now().isBefore(event.getTimeToLive()))
			sendEvent(event);
		else
			expireEvent(event);
	}

	private void sendEvent(final EbMSEvent event)
	{
		val receiveDeliveryChannel = cpaManager.getDeliveryChannel(
				event.getCpaId(),
				event.getReceiveDeliveryChannelId())
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",event.getCpaId(),event.getReceiveDeliveryChannelId()));
		val url = urlMapper.getURL(CPAUtils.getUri(receiveDeliveryChannel));
		try
		{
			val requestDocument = ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId());
			StreamUtils.ifPresentOrElse(requestDocument,
					d -> sendMessage(event,receiveDeliveryChannel,url,d),
					() -> eventManager.deleteEvent(event.getMessageId()));
		}
		catch (final EbMSResponseException e)
		{
			log.error("",e);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,e.getMessage());
						if ((e instanceof EbMSUnrecoverableResponseException) || !CPAUtils.isReliableMessaging(receiveDeliveryChannel))
							if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
							{
								eventListener.onMessageFailed(event.getMessageId());
								if (deleteEbMSAttachmentsOnMessageProcessed)
									ebMSDAO.deleteAttachments(event.getMessageId());
							}
					}
				}
			);
		}
		catch (final Exception e)
		{
			log.error("",e);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						eventManager.updateEvent(event,url,EbMSEventStatus.FAILED,ExceptionUtils.getStackTrace(e));
						if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
							if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERY_FAILED) > 0)
							{
								eventListener.onMessageFailed(event.getMessageId());
								if (deleteEbMSAttachmentsOnMessageProcessed)
									ebMSDAO.deleteAttachments(event.getMessageId());
							}
					}
				}
			);
		}
	}

	private void sendMessage(final EbMSEvent event, DeliveryChannel receiveDeliveryChannel, final String url, EbMSDocument requestDocument)
	{
		try
		{
			if (event.isConfidential())
				messageEncrypter.encrypt(receiveDeliveryChannel,requestDocument);
			log.info("Sending message " + event.getMessageId() + " to " + url);
			val responseDocument = createClient(event).sendMessage(url,requestDocument);
			messageProcessor.processResponse(requestDocument,responseDocument);
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						eventManager.updateEvent(event,url,EbMSEventStatus.SUCCEEDED);
						if (!CPAUtils.isReliableMessaging(receiveDeliveryChannel))
							if (ebMSDAO.updateMessage(event.getMessageId(),EbMSMessageStatus.SENDING,EbMSMessageStatus.DELIVERED) > 0)
							{
								eventListener.onMessageDelivered(event.getMessageId());
								if (deleteEbMSAttachmentsOnMessageProcessed)
									ebMSDAO.deleteAttachments(event.getMessageId());
							}
					}
				});
		}
		catch (CertificateException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	private EbMSClient createClient(EbMSEvent event) throws CertificateException
	{
		String cpaId = event.getCpaId();
		val sendDeliveryChannel = event.getSendDeliveryChannelId() != null ?
				cpaManager.getDeliveryChannel(cpaId,event.getSendDeliveryChannelId())
				.orElse(null) : null;
		return ebMSClientFactory.getEbMSClient(cpaId,sendDeliveryChannel);
	}

	private void expireEvent(final EbMSEvent event)
	{
		try
		{
			log.warn("Expiring message " +  event.getMessageId());
			ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						ebMSDAO.getEbMSDocumentIfUnsent(event.getMessageId()).ifPresent(d -> updateMessage(event.getMessageId()));
						eventManager.deleteEvent(event.getMessageId());
					}

					private void updateMessage(final String messageId)
					{
						if (ebMSDAO.updateMessage(messageId,EbMSMessageStatus.SENDING,EbMSMessageStatus.EXPIRED) > 0)
						{
							eventListener.onMessageExpired(messageId);
							if (deleteEbMSAttachmentsOnMessageProcessed)
								ebMSDAO.deleteAttachments(messageId);
						}
					}
				}
			);
		}
		catch (Exception e)
		{
			log.error("",e);
		}
	}
}
