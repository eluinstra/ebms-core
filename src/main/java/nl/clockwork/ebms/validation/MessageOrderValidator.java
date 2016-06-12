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
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.StatusType;

public class MessageOrderValidator
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected final long MIN_SEQUENCE_NR = 0L;
	protected final long MAX_SEQUENCE_NR = 99999999L;
	protected EbMSDAO ebMSDAO;
	protected CPAManager cpaManager;

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		MessageHeader messageHeader = message.getMessageHeader();
		MessageOrder messageOrder = message.getMessageOrder();

		MessageOrderSemanticsType messageOrderSemantics = cpaManager.getMessageOrderSemantics(messageHeader.getCPAId(),new CacheablePartyId(messageHeader.getTo().getPartyId()),messageHeader.getTo().getRole(),CPAUtils.toString(messageHeader.getService()),messageHeader.getAction());
		if (messageOrder != null)
		{
			if (!MessageOrderSemanticsType.GUARANTEED.equals(messageOrderSemantics))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.INCONSISTENT,"Element not allowed."));
			if (message.getSyncReply() != null)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.INCONSISTENT,"SyncReply not allowed."));
			if (!Constants.EBMS_VERSION.equals(messageOrder.getVersion()))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/@version",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			long sequenceNr = messageOrder.getSequenceNumber().getValue().longValue();
			EbMSMessageContext lastMessage = ebMSDAO.getLastReceivedMessage(messageHeader.getCPAId(),messageHeader.getConversationId());
			if (sequenceNr > MIN_SEQUENCE_NR)
			{
				if (lastMessage != null)
				{
					if (StatusType.RESET.equals(messageOrder.getSequenceNumber().getStatus()))
						throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber/@status",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Value not allowed."));
					if (!EbMSMessageStatus.RECEIVED.equals(lastMessage.getMessageStatus()) && !EbMSMessageStatus.PROCESSED.equals(lastMessage.getMessageStatus()))
					{
						if (lastMessage.getSequenceNr() == sequenceNr - 1)
							return;
						else if (lastMessage.getSequenceNr() < sequenceNr - 1)
							throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber " + (lastMessage.getSequenceNr() + 1) + "."));
						else //if (context.getSequenceNr() > sequenceNr - 1)
							throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + lastMessage.getSequenceNr() + " already received."));
					}
					else
						throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + lastMessage.getSequenceNr() + " failed with status " + lastMessage.getMessageStatus().statusCode() + "."));
				}
				else
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Missing message with SequenceNumber 0."));
			}
			else if (sequenceNr < MIN_SEQUENCE_NR)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else if (sequenceNr > MAX_SEQUENCE_NR)
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.INCONSISTENT,"Invalid value."));
			else //if (sequenceNr == MIN_SEQUENCE_NR)
			{
				if (lastMessage != null)
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Message with SequenceNumber " + lastMessage.getSequenceNr() + " already received."));
				if (StatusType.CONTINUE.equals(messageOrder.getSequenceNumber().getStatus()))
					throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder/SequenceNumber/@status",Constants.EbMSErrorCode.DELIVERY_FAILURE,"Value not supported."));
			}
		}
		else
			if (MessageOrderSemanticsType.GUARANTEED.equals(messageOrderSemantics))
				throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.INCONSISTENT,"Element not found."));

	}

	public void generateSequenceNr(EbMSMessageContext messageContext) throws ValidationException
	{
		if (MessageOrderSemanticsType.GUARANTEED.equals(cpaManager.getMessageOrderSemantics(messageContext.getCpaId(),new CacheablePartyId(messageContext.getToRole().getPartyId()),messageContext.getToRole().getRole(),messageContext.getService(),messageContext.getAction())))
		{
			if (messageContext.getConversationId() != null)
			{
				EbMSMessageContext lastMessage = ebMSDAO.getLastSentMessage(messageContext.getCpaId(),messageContext.getConversationId());
				messageContext.setSequenceNr(lastMessage == null ? MIN_SEQUENCE_NR : createNextSequenceNr(lastMessage));
			}
			else
				messageContext.setSequenceNr(MIN_SEQUENCE_NR);
		}
		else
			messageContext.setSequenceNr(null);
	}

	private Long createNextSequenceNr(EbMSMessageContext lastMessage) throws ValidationException
	{
		Long result = lastMessage.getSequenceNr() + 1;
		if (result.compareTo(MAX_SEQUENCE_NR) > 0)
			throw new ValidationException("Max SequenceNr reached.");
		return result;
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
