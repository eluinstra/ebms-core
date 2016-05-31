package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;

public class MessageOrderValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		MessageOrder messageOrder = message.getMessageOrder();

		//check if messageOrder is needed according to CPA.
		if (messageOrder != null)
		{
			if (!Constants.EBMS_VERSION.equals(messageOrder.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			long sequenceNr = messageOrder.getSequenceNumber().getValue().longValue();
			EbMSMessageContext context = ebMSDAO.getLastReceivedMessage(messageHeader.getCPAId(),messageHeader.getConversationId());
			if (sequenceNr > 0)
			{
				if (context != null)
				{
					if (context.getSequenceNr() == sequenceNr - 1)
						return;
					else if (context.getSequenceNr() < sequenceNr - 1)
						throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber " + (context.getSequenceNr() + 1) + "."));
					else //if (context.getSequenceNr() > sequenceNr - 1)
						throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"SequenceNumber already exists."));
				}
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber 0."));
			}
			else if (sequenceNr < 0)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else if (sequenceNr > 99999999L)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else
				if (context != null)
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"SequenceNumber already exists."));
		}
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}
}
