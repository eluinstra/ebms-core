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
import java.util.Date;
import java.util.List;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.DeliveryChannel;
import nl.clockwork.mule.ebms.model.cpp.cpa.PartyInfo;
import nl.clockwork.mule.ebms.model.cpp.cpa.PerMessageCharacteristicsType;
import nl.clockwork.mule.ebms.model.cpp.cpa.PersistenceLevelType;
import nl.clockwork.mule.ebms.model.cpp.cpa.StatusValueType;
import nl.clockwork.mule.ebms.model.cpp.cpa.SyncReplyModeType;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.PartyId;
import nl.clockwork.mule.ebms.model.ebxml.Service;
import nl.clockwork.mule.ebms.util.CPAUtils;
import nl.clockwork.mule.ebms.util.EbMSMessageUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

public class EbMSMessageHeaderValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;
	private PersistenceLevelType isAuthenticated = PersistenceLevelType.PERSISTENT;
	private boolean isAuthorizationRequired = true;
	private PersistenceLevelType isConfidential = PersistenceLevelType.NONE;
	private SyncReplyModeType syncReplyMode = SyncReplyModeType.NONE;
	private PerMessageCharacteristicsType ackRequested = PerMessageCharacteristicsType.ALWAYS;
	private PerMessageCharacteristicsType ackSignatureRequested = PerMessageCharacteristicsType.NEVER;
	private PerMessageCharacteristicsType duplicateElimination = PerMessageCharacteristicsType.ALWAYS;

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

				if (cpa == null)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),""));
					return false;
				}
				if (!StatusValueType.AGREED.equals(cpa.getStatus().getValue()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"CPA not agreed."));
					return false;
				}
				Date now = new Date();
				if (now.compareTo(cpa.getStart().toGregorianCalendar().getTime()) < 0 || now.compareTo(cpa.getEnd().toGregorianCalendar().getTime()) > 0)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@cpaid]",Constants.EbMSErrorCode.VALUE_NOT_RECOGNIZED.errorCode(),"CPA expired."));
					return false;
				}

				if (!Constants.EBMS_VERSION.equals(messageHeader.getVersion()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				if (true != messageHeader.isMustUnderstand())
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader[@mustUnderstand]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}

				if (!isValid(messageHeader.getFrom().getPartyId()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				if ((from = CPAUtils.getPartyInfo(cpa, messageHeader.getFrom().getPartyId())) == null)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/From/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found."));
					return false;
				}
				if (!CPAUtils.canSend(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found."));
					return false;
				}

				if (!isValid(messageHeader.getTo().getPartyId()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				if ((to = CPAUtils.getPartyInfo(cpa, messageHeader.getTo().getPartyId())) == null)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/To/PartyId",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value not found."));
					return false;
				}
				if (!CPAUtils.canReceive(to,messageHeader.getTo().getRole(),messageHeader.getService(),messageHeader.getAction()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Action",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Action not found."));
					return false;
				}

				if (!isValid(messageHeader.getService()))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/Service",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Value invalid."));
					return false;
				}
				
				List<DeliveryChannel> deliveryChannels = CPAUtils.getDeliveryChannels(from,messageHeader.getFrom().getRole(),messageHeader.getService(),messageHeader.getAction());
				if (deliveryChannels.size() == 0)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.UNKNOWN.errorCode(),"No DeviveryChannel found."));
					return false;
				}
				if (deliveryChannels.size() > 1)
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"Multiple DeviveryChannels not supported."));
					return false;
				}
				DeliveryChannel deliveryChannel = deliveryChannels.get(0);
				if (!checkDuplicateElimination(deliveryChannel))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"DuplicateElimination value not supported."));
					return false;
				}
				if (!checkDuplicateElimination(deliveryChannel,messageHeader))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/MessageHeader/DuplicateElimination",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
					return false;
				}
				if (!checkSyncReplyMode(deliveryChannel))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"SyncReplyMode not supported."));
					return false;
				}
				if (!checkAckRequested(deliveryChannel))
				{
					message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"AckRequested mode not supported."));
					return false;
				}
				if (message.getPayload() instanceof EbMSMessage)
				{
					AckRequested ackRequested = ((EbMSMessage)message.getPayload()).getAckRequested();
					if (!Constants.EBMS_VERSION.equals(ackRequested.getVersion()))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@version]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (true != ackRequested.isMustUnderstand())
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested[@mustUnderstand]",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (!checkAckRequested(deliveryChannel,ackRequested))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError("//Header/AckRequested",Constants.EbMSErrorCode.INCONSISTENT.errorCode(),"Wrong value."));
						return false;
					}
					if (!checkAckSignatureRequested(deliveryChannel))
					{
						message.setProperty(Constants.EBMS_ERROR,EbMSMessageUtils.createError(Constants.EbMSErrorCode.UNKNOWN.errorCode(),Constants.EbMSErrorCode.NOT_SUPPORTED.errorCode(),"AckSignatureRequested mode not supported."));
						return false;
					}
				}
				if (message.getPayload() instanceof EbMSAcknowledgment)
				{
					//TODO check for AckSignature???
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
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(duplicateElimination);
	}
	
	private boolean checkDuplicateElimination(DeliveryChannel deliveryChannel, MessageHeader messageHeader)
	{
		return deliveryChannel.getMessagingCharacteristics().getDuplicateElimination().equals(PerMessageCharacteristicsType.ALWAYS)
				&& messageHeader.getDuplicateElimination() != null;
	}
	
	private boolean checkSyncReplyMode(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getSyncReplyMode().equals(syncReplyMode);
	}
	
	private boolean checkAckRequested(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckRequested().equals(ackRequested);
	}

	private boolean checkAckRequested(DeliveryChannel deliveryChannel, AckRequested ackRequested)
	{
		return (this.ackRequested.equals(PerMessageCharacteristicsType.ALWAYS) && ackRequested != null)
				|| (this.ackRequested.equals(PerMessageCharacteristicsType.PER_MESSAGE))
				|| (this.ackRequested.equals(PerMessageCharacteristicsType.NEVER) && ackRequested == null)
		;
	}
	
	private boolean checkAckSignatureRequested(DeliveryChannel deliveryChannel)
	{
		return deliveryChannel.getMessagingCharacteristics().getAckSignatureRequested().equals(ackSignatureRequested);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setIsAuthenticated(PersistenceLevelType isAuthenticated)
	{
		this.isAuthenticated = isAuthenticated;
	}
	
	public void setAuthorizationRequired(boolean isAuthorizationRequired)
	{
		this.isAuthorizationRequired = isAuthorizationRequired;
	}
	
	public void setIsConfidential(PersistenceLevelType isConfidential)
	{
		this.isConfidential = isConfidential;
	}
	
	public void setSyncReplyMode(SyncReplyModeType syncReplyMode)
	{
		this.syncReplyMode = syncReplyMode;
	}
	
	public void setAckRequested(PerMessageCharacteristicsType ackRequested)
	{
		this.ackRequested = ackRequested;
	}
	
	public void setAckSignatureRequested(PerMessageCharacteristicsType ackSignatureRequested)
	{
		this.ackSignatureRequested = ackSignatureRequested;
	}
	
	public void setDuplicateElimination(PerMessageCharacteristicsType duplicateElimination)
	{
		this.duplicateElimination = duplicateElimination;
	}
}
