package nl.clockwork.ebms.validation;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessageOrderSemanticsType;
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

		MessageOrderSemanticsType messageOrderSemantics = cpaManager.getMessageOrderSemantics(cpaManager.getReceiveDeliveryChannel(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId()),messageHeader.getTo().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction()));
		if (messageOrder != null)
		{
			if (!MessageOrderSemanticsType.GUARANTEED.equals(messageOrderSemantics))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.INCONSISTENT,"Element not allowed."));
			if (!Constants.EBMS_VERSION.equals(messageOrder.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			long sequenceNr = messageOrder.getSequenceNumber().getValue().longValue();
			EbMSMessageContext context = ebMSDAO.getLastReceivedMessage(messageHeader.getCPAId(),messageHeader.getConversationId());
			if (sequenceNr > 0)
			{
				if (context != null)
				{
					if (!EbMSMessageStatus.RECEIVED.equals(context.getMessageStatus()) && !EbMSMessageStatus.PROCESSED.equals(context.getMessageStatus()))
					{
						if (context.getSequenceNr() == sequenceNr - 1)
							return;
						else if (context.getSequenceNr() < sequenceNr - 1)
							throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber " + (context.getSequenceNr() + 1) + "."));
						else //if (context.getSequenceNr() > sequenceNr - 1)
							throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + context.getSequenceNr() + " already received."));
					}
					else
						throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + context.getSequenceNr() + " failed with status " + context.getMessageStatus().statusCode() + "."));
				}
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber 0."));
			}
			else if (sequenceNr < 0)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else if (sequenceNr > 99999999L)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else //if (sequenceNr == 0)
				if (context != null)
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + context.getSequenceNr() + " already received."));
		}
		else
			if (MessageOrderSemanticsType.GUARANTEED.equals(messageOrderSemantics))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.INCONSISTENT,"Element not found."));

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
