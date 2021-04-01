package nl.clockwork.ebms.processor;

import java.io.IOException;
import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.MessageEventListener;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.validation.DuplicateMessageException;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.EbMSValidationException;
import nl.clockwork.ebms.validation.ValidatorException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class MessageProcessor
{
  @NonNull
  MessageEventListener messageEventListener;
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	DuplicateMessageHandler duplicateMessageHandler;
  @NonNull
	MessageErrorProcessor messageErrorProcessor;
  @NonNull
	AcknowledgmentProcessor acknowledgmentProcessor;

  public EbMSDocument processMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message) throws ValidatorException, DatatypeConfigurationException, JAXBException, SOAPException, ParserConfigurationException, SAXException, IOException, TransformerFactoryConfigurationError, TransformerException, EbMSProcessorException
	{
		try
		{
			messageValidator.validateAndDecryptMessage(messageDocument,message,timestamp);
			if (message.getAckRequested() == null)
			{
				storeReceivedMessage(timestamp,messageDocument,message);
				return null;
			}
			else
			{
				val acknowledgmentDocument = acknowledgmentProcessor.processAcknowledgment(timestamp,messageDocument,message,message.isSyncReply(cpaManager));
				return message.isSyncReply(cpaManager) ? acknowledgmentDocument : null;
			}
		}
		catch (DuplicateMessageException e)
		{
			return duplicateMessageHandler.handleMessage(timestamp,messageDocument,message);
		}
		catch (final EbMSValidationException e)
		{
			log.warn("Invalid message " + message.getMessageHeader().getMessageData().getMessageId() + "\n" + e.getMessage());
			val messageErrorDocument = messageErrorProcessor.processMessageError(timestamp,messageDocument,message,message.isSyncReply(cpaManager),e);
			return message.isSyncReply(cpaManager) ? messageErrorDocument : null;
		}
	}

	public void storeReceivedMessage(final Instant timestamp, final EbMSDocument messageDocument, final EbMSMessage message)
	{
		ebMSDAO.insertMessage(timestamp,null,messageDocument.getMessage(),message,message.getAttachments(),EbMSMessageStatus.RECEIVED);
		messageEventListener.onMessageReceived(message.getMessageHeader().getMessageData().getMessageId());
	}

	public void processDeliveredMessage(final EbMSMessage message, boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		val messageHeader = message.getMessageHeader();
		if (ebMSDAO.updateMessage(
				messageHeader .getMessageData().getMessageId(),
				EbMSMessageStatus.CREATED,
				EbMSMessageStatus.DELIVERED) > 0)
		{
			messageEventListener.onMessageDelivered(messageHeader.getMessageData().getMessageId());
			if (deleteEbMSAttachmentsOnMessageProcessed)
				ebMSDAO.deleteAttachments(messageHeader.getMessageData().getMessageId());
		}
	}

}
