package nl.clockwork.ebms.processor;

import java.util.Date;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.common.EbMSMessageFactory;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.job.EventManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.validation.EbMSMessageValidator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;

public class DuplicateMessageHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
  protected EbMSDAO ebMSDAO;
  protected CPAManager cpaManager;
  protected EbMSMessageFactory ebMSMessageFactory;
	protected EventManager eventManager;
	protected EbMSMessageValidator messageValidator;
	protected Service mshMessageService;

	public DuplicateMessageHandler()
	{
		mshMessageService = new Service();
		mshMessageService.setValue(Constants.EBMS_SERVICE_URI);
	}

	public EbMSDocument handleMessage(final Date timestamp, final EbMSMessage message, final MessageHeader messageHeader) throws EbMSProcessingException
	{
		if (isIdenticalMessage(message))
		{
			logger.warn("Message " + message.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			if (messageValidator.isSyncReply(message))
			{
				ebMSDAO.insertDuplicateMessage(timestamp,message);
				EbMSDocument result = ebMSDAO.getEbMSDocumentByRefToMessageId(messageHeader.getCPAId(),messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
				if (result == null)
					logger.warn("No response found for duplicate message " + message.getMessageHeader().getMessageData().getMessageId() + "!");
				return result;
			}
			else
			{
				ebMSDAO.executeTransaction(
					new DAOTransactionCallback()
					{
						@Override
						public void doInTransaction()
						{
							ebMSDAO.insertDuplicateMessage(timestamp,message);
							EbMSMessageContext messageContext = ebMSDAO.getMessageContextByRefToMessageId(messageHeader.getCPAId(),messageHeader.getMessageData().getMessageId(),mshMessageService,EbMSAction.MESSAGE_ERROR.action(),EbMSAction.ACKNOWLEDGMENT.action());
							if (messageContext != null)
								eventManager.createEvent(messageHeader.getCPAId(),cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(message.getMessageHeader().getFrom().getPartyId()),message.getMessageHeader().getFrom().getRole(),CPAUtils.toString(CPAUtils.createEbMSMessageService()),null),messageContext.getMessageId(),message.getMessageHeader().getMessageData().getTimeToLive(),messageContext.getTimestamp(),false);
							else
								logger.warn("No response found for duplicate message " + message.getMessageHeader().getMessageData().getMessageId() + "!");
						}
					}
				);
				return null;
			}
		}
		else
			throw new EbMSProcessingException("MessageId " + message.getMessageHeader().getMessageData().getMessageId() + " already used!");
	}

	public void handleMessageError(final Date timestamp, final EbMSMessage responseMessage) throws EbMSProcessingException
	{
		if (isIdenticalMessage(responseMessage))
		{
			logger.warn("MessageError " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
		}
		else
			throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
	}
	
	public void handleAcknowledgment(final Date timestamp, final EbMSMessage responseMessage) throws EbMSProcessingException
	{
		if (isIdenticalMessage(responseMessage))
		{
			logger.warn("Acknowledgment " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " is duplicate!");
			ebMSDAO.insertDuplicateMessage(timestamp,responseMessage);
		}
		else
			throw new EbMSProcessingException("MessageId " + responseMessage.getMessageHeader().getMessageData().getMessageId() + " already used!");
	}
	
	private boolean isIdenticalMessage(EbMSMessage message)
	{
		return ebMSDAO.existsIdenticalMessage(message);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setEbMSMessageFactory(EbMSMessageFactory ebMSMessageFactory)
	{
		this.ebMSMessageFactory = ebMSMessageFactory;
	}

	public void setEventManager(EventManager eventManager)
	{
		this.eventManager = eventManager;
	}

	public void setMessageValidator(EbMSMessageValidator messageValidator)
	{
		this.messageValidator = messageValidator;
	}
}
