/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.filter;

import java.net.URI;
import java.util.GregorianCalendar;
import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.cpp.cpa.ActorType;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.ebms.model.cpp.cpa.PerMessageCharacteristicsType;
import nl.clockwork.ebms.model.cpp.cpa.SyncReplyModeType;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.PartyId;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSMessageHeaderValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private PerMessageCharacteristicsType duplicateElimination = PerMessageCharacteristicsType.ALWAYS;
	private PerMessageCharacteristicsType ackRequested = PerMessageCharacteristicsType.ALWAYS;
	private PerMessageCharacteristicsType ackSignatureRequested = PerMessageCharacteristicsType.NEVER;
	private SyncReplyModeType syncReplyMode = SyncReplyModeType.NONE;

	@Override
	public boolean accept(MuleMessage message)
	{
		if (message.getPayload() instanceof EbMSBaseMessage)
		{
			try
			{
				EbMSBaseMessage msg = (EbMSBaseMessage)message.getPayload();
				MessageHeader messageHeader = msg.getMessageHeader();
				CollaborationProtocolAgreement cpa = ebMSDAO.getCPA(messageHeader.getCPAId());
				PartyInfo from = null;
				PartyInfo to = null;

				if (!Constants.EBMS_VERSION.equals(messageHeader.getVersion()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}

				if (!isValid(messageHeader.getFrom().getPartyId()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				if (!isValid(messageHeader.getTo().getPartyId()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				if (!isValid(messageHeader.getService()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Service",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				
				if ((from = CPAUtils.getPartyInfo(cpa, messageHeader.getFrom().getPartyId())) == null)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found."));
					return false;
				}
				if ((to = CPAUtils.getPartyInfo(cpa, messageHeader.getTo().getPartyId())) == null)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found."));
					return false;
				}

				if (!CPAUtils.canSend(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"Value not found."));
					return false;
				}
				if (!CPAUtils.canReceive(to,messageHeader.getTo().getRole(),messageHeader.getService(),messageHeader.getAction()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"Value not found."));
					return false;
				}

				List<DeliveryChannel> deliveryChannels = CPAUtils.getDeliveryChannels(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
				if (deliveryChannels.size() == 0)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"No DeliveryChannel found."));
					return false;
				}
				if (deliveryChannels.size() > 1)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"Multiple DeliveryChannels not supported."));
					return false;
				}
				DeliveryChannel deliveryChannel = deliveryChannels.get(0);
				if (!checkTimeToLive(messageHeader))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/MessageData/TimeToLive",Constants.EbMSErrorCode.TIME_TO_LIVE_EXPIRED.errorCode(),null));
					return false;
				}
				if (message.getPayload() instanceof EbMSMessage)
				{
					if (!checkDuplicateElimination(deliveryChannel))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"DuplicateElimination mode not supported."));
						return false;
					}
					if (!checkDuplicateElimination(deliveryChannel,messageHeader))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					AckRequested ackRequested = ((EbMSMessage)message.getPayload()).getAckRequested();
					if (!checkAckRequested(deliveryChannel))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"AckRequested mode not supported."));
						return false;
					}
					if (!checkAckRequested(deliveryChannel,ackRequested))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (ackRequested != null && !Constants.EBMS_VERSION.equals(ackRequested.getVersion()))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (ackRequested != null && ackRequested.getActor() != null && ackRequested.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@actor]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"NextMSH not supported."));
						return false;
					}
					if (!checkAckSignatureRequested(deliveryChannel))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@signed]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"Signed Acknowledgment mode not supported."));
						return false;
					}
					if (!checkAckSignatureRequested(deliveryChannel,ackRequested))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@signed]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					SyncReply syncReply = ((EbMSMessage)message.getPayload()).getSyncReply();
					if (!checkSyncReply(deliveryChannel))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReply mode not supported."));
						return false;
					}
					if (!checkSyncReply(deliveryChannel,syncReply))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/SyncReply",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (syncReply != null && !Constants.EBMS_VERSION.equals(syncReply.getVersion()))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/SyncReply[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (syncReply != null && syncReply.getActor() != null && !syncReply.getActor().equals("http://schemas.xmlsoap.org/soap/actor/next"))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/SyncReply[@actor]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					MessageOrder messageOrder = ((EbMSMessage)message.getPayload()).getMessageOrder();
					if (messageOrder != null)
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageOrder",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"MessageOrder not supported."));
						return false;
					}
				}
				if (message.getPayload() instanceof EbMSAcknowledgment)
				{
					Acknowledgment acknowledgment = ((EbMSAcknowledgment)message.getPayload()).getAcknowledgment();
					if (!checkActor(deliveryChannel,acknowledgment))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Acknowledgment[@actor]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (acknowledgment.getActor() != null && acknowledgment.getActor().equals(ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.value()))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Acknowledgment[@actor]",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"NextMSH not supported."));
						return false;
					}
					if (!checkAckSignatureRequested(deliveryChannel,acknowledgment))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/Acknowledgment/Reference",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
				}
				return true;
			}
			catch (DAOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return true;
	}

	private boolean isValid(List<PartyId> partyIds)
	{
		for (PartyId partyId : partyIds)
			if (!StringUtils.isEmpty(partyId.getType()) || isValidURI(partyId.getValue())/*FIXME replace by: org.apache.commons.validator.UrlValidator.isValid(partyId.getValue())*/)
				return true;
		return false;
	}

	private boolean isValidURI(String s)
	{
		try
		{
			new URI(s);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean isValid(Service service)
	{
		return !StringUtils.isEmpty(service.getType()) || isValidURI(service.getValue())/*FIXME replace by: org.apache.commons.validator.UrlValidator.isValid(service.getValue())*/; 
	}
	
	private boolean checkTimeToLive(MessageHeader messageHeader)
	{
		return messageHeader.getMessageData().getTimeToLive() == null
				|| new GregorianCalendar().before(messageHeader.getMessageData().getTimeToLive().toGregorianCalendar());
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel, MessageHeader messageHeader)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination() == null || deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.NEVER) && messageHeader.getDuplicateElimination() == null)
				|| (deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.ALWAYS) && messageHeader.getDuplicateElimination() != null);
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(duplicateElimination);
	}
	
	private boolean checkAckRequested(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(ackRequested);
	}

	private boolean checkAckRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested() == null || deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested != null)
				|| (deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(PerMessageCharacteristicsType.NEVER) && ackRequested == null)
		;
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(ackSignatureRequested);
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested.isSigned())
				|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
				|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER) && !ackRequested.isSigned());
	}

	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
	{
		return (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.ALWAYS) && (acknowledgment.getReference() != null))
		|| deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.PER_MESSAGE)
		|| (deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(PerMessageCharacteristicsType.NEVER));
	}

	private boolean checkSyncReply(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(syncReplyMode);
	}
	
	private boolean checkSyncReply(DeliveryChannel deliveryChannel, SyncReply syncReply)
	{
		return !((deliveryChannel.getMessagingCharacteristics().getSyncReplyMode() == null || deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(SyncReplyModeType.NONE))
				&& syncReply != null);
	}
	
	private boolean checkActor(DeliveryChannel deliveryChannel, Acknowledgment acknowledgment)
	{
		return (deliveryChannel.getMessagingCharacteristics().getActor() == null && acknowledgment.getActor() == null) 
				|| deliveryChannel.getMessagingCharacteristics().getActor().value().equals(acknowledgment.getActor());
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
