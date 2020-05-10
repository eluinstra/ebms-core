package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.common.util.StreamUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.listener.EventListener;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidatorException;

@Builder
@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class AcknowledgmentProcessor
{
	@NonNull
	EbMSDAO ebMSDAO;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EventManager eventManager;
	@NonNull
	EbMSMessageValidator messageValidator;
	@NonNull
	DuplicateMessageHandler duplicateMessageHandler;
	@NonNull
	EbMSMessageFactory ebMSMessageFactory;
	@NonNull
	EbMSSignatureGenerator signatureGenerator;
	@NonNull
	EventListener eventListener;
	boolean deleteEbMSAttachmentsOnMessageProcessed;

	public EbMSDocument processAcknowledgment(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message, final boolean isSyncReply) throws SOAPException, JAXBException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException
	{
		val acknowledgment = createAcknowledgment(timestamp,messageDocument,message);
		val acknowledgmentDocument = EbMSMessageUtils.getEbMSDocument(acknowledgment);
		signatureGenerator.generate(message.getAckRequested(),acknowledgmentDocument,acknowledgment);
		ebMSDAO.executeTransaction(
				new DAOTransactionCallback()
				{
					@Override
					public void doInTransaction()
					{
						storeMessages(timestamp,messageDocument,message,acknowledgmentDocument,acknowledgment);
						storeEvent(message,acknowledgment,isSyncReply);
						eventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
					}

					private void storeMessages(Instant timestamp, EbMSDocument messageDocument, EbMSMessage message, EbMSDocument acknowledgmentDocument, EbMSAcknowledgment acknowledgment)
					{
						val messageHeader = message.getMessageHeader();
						val toPartyId = new CacheablePartyId(message.getMessageHeader().getTo().getPartyId());
						val service = CPAUtils.toString(message.getMessageHeader().getService());
						val deliveryChannel =
								cpaManager.getReceiveDeliveryChannel(messageHeader .getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction())
								.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,messageHeader.getTo().getRole(),service,messageHeader.getAction()));
						val persistTime = CPAUtils.getPersistTime(messageHeader.getMessageData().getTimestamp(),deliveryChannel);
						ebMSDAO.insertMessage(timestamp,persistTime,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.RECEIVED);
						ebMSDAO.insertMessage(timestamp,persistTime,acknowledgmentDocument.getMessage(),acknowledgment,Collections.emptyList(),null);
					}

					private void storeEvent(EbMSMessage message, EbMSAcknowledgment acknowledgment, boolean isSyncReply)
					{
						val messageHeader = message.getMessageHeader();
						val fromPartyId = new CacheablePartyId(acknowledgment.getMessageHeader().getFrom().getPartyId());
						val toPartyId = new CacheablePartyId(acknowledgment.getMessageHeader().getTo().getPartyId());
						val service = CPAUtils.toString(acknowledgment.getMessageHeader().getService());
						val sendDeliveryChannel =
								cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),fromPartyId,acknowledgment.getMessageHeader().getFrom().getRole(),service,acknowledgment.getMessageHeader().getAction())
								.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",messageHeader.getCPAId(),fromPartyId,acknowledgment.getMessageHeader().getFrom().getRole(),service,acknowledgment.getMessageHeader().getAction()));
						val receiveDeliveryChannel =
								cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction())
								.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",messageHeader.getCPAId(),toPartyId,acknowledgment.getMessageHeader().getTo().getRole(),service,acknowledgment.getMessageHeader().getAction()));
						if (!isSyncReply)
							eventManager.createEvent(
									messageHeader.getCPAId(),
									sendDeliveryChannel,
									receiveDeliveryChannel,
									acknowledgment.getMessageHeader().getMessageData().getMessageId(),
									acknowledgment.getMessageHeader().getMessageData().getTimeToLive(),
									acknowledgment.getMessageHeader().getMessageData().getTimestamp(),
									false);
					}
				}
		);
		return acknowledgmentDocument;
	}

	public void processAcknowledgment(Instant timestamp, EbMSDocument acknowledgmentDocument, EbMSMessage requestMessage, EbMSAcknowledgment acknowledgment) throws XPathExpressionException, JAXBException, ParserConfigurationException, SAXException, IOException
	{
		try
		{
			messageValidator.validateAcknowledgment(acknowledgmentDocument,requestMessage,acknowledgment,timestamp);
			storeAcknowledgment(timestamp,requestMessage,acknowledgmentDocument,acknowledgment);
		}
		catch (DuplicateMessageException e)
		{
			duplicateMessageHandler.handleAcknowledgment(timestamp,acknowledgmentDocument,acknowledgment);
		}
		catch (ValidatorException e)
		{
			val persistTime = ebMSDAO.getPersistTime(acknowledgment.getMessageHeader().getMessageData().getRefToMessageId());
			ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),acknowledgmentDocument.getMessage(),acknowledgment,Collections.emptyList(),null);
			log.warn("Unable to process Acknowledgment " + acknowledgment.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	public EbMSAcknowledgment createAcknowledgment(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message)
	{
		return ebMSMessageFactory.createEbMSAcknowledgment(message,timestamp);
	}

	public void storeAcknowledgment(final Instant timestamp, final EbMSMessage message, final EbMSDocument acknowledgmentDocument, final EbMSAcknowledgment acknowledgment) throws EbMSProcessingException
	{
		ebMSDAO.executeTransaction(
			new DAOTransactionCallback()
			{
				@Override
				public void doInTransaction()
				{
					val responseMessageHeader = acknowledgment.getMessageHeader();
					val persistTime = ebMSDAO.getPersistTime(responseMessageHeader.getMessageData().getRefToMessageId());
					ebMSDAO.insertMessage(timestamp,persistTime.orElse(null),acknowledgmentDocument.getMessage(),acknowledgment,Collections.emptyList(),null);
					if (ebMSDAO.updateMessage(
							responseMessageHeader.getMessageData().getRefToMessageId(),
							EbMSMessageStatus.SENDING,
							EbMSMessageStatus.DELIVERED) > 0)
					{
						eventListener.onMessageDelivered(responseMessageHeader.getMessageData().getRefToMessageId());
						if (deleteEbMSAttachmentsOnMessageProcessed)
							ebMSDAO.deleteAttachments(responseMessageHeader.getMessageData().getRefToMessageId());
					}
				}
			}
		);
	}
}
