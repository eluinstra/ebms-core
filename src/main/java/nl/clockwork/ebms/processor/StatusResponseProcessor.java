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
package nl.clockwork.ebms.processor;

import java.time.Instant;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import org.javatuples.Pair;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.cpa.CacheablePartyId;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidatorException;

@Slf4j
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class StatusResponseProcessor
{
  @NonNull
	EbMSDAO ebMSDAO;
  @NonNull
	CPAManager cpaManager;
  @NonNull
	EbMSMessageValidator messageValidator;
  @NonNull
	EbMSMessageFactory ebMSMessageFactory;
  @NonNull
  DeliveryManager deliveryManager;

  public EbMSStatusResponse createStatusResponse(final EbMSStatusRequest statusRequest, final Instant timestamp) throws ValidatorException, DatatypeConfigurationException, JAXBException, EbMSProcessorException
	{
		val mc = ebMSDAO.getMessageContext(statusRequest.getStatusRequest().getRefToMessageId()).orElse(null);
		val result = createEbMSMessageStatusAndTimestamp(statusRequest,mc);
		return ebMSMessageFactory.createEbMSStatusResponse(statusRequest,result.getValue0(),result.getValue1()); 
	}
	
  public void sendStatusResponse(final nl.clockwork.ebms.model.EbMSStatusResponse statusResponse)
	{
		val messageHeader = statusResponse.getMessageHeader();
		val toPartyId = new CacheablePartyId(messageHeader.getTo().getPartyId());
		val service = CPAUtils.toString(messageHeader.getService());
		val uri = cpaManager.getUri(
				messageHeader.getCPAId(),
				toPartyId,
				messageHeader.getTo().getRole(),
				service,
				messageHeader.getAction());
		deliveryManager.sendResponseMessage(uri,statusResponse);
	}
	
  public void processStatusResponse(Instant timestamp, EbMSStatusResponse statusResponse)
	{
		try
		{
			messageValidator.validate(statusResponse,timestamp);
			deliveryManager.handleResponseMessage(statusResponse);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process StatusResponse " + statusResponse.getMessageHeader().getMessageData().getMessageId(),e);
		}
	}

	private Pair<EbMSMessageStatus,Instant> createEbMSMessageStatusAndTimestamp(EbMSStatusRequest statusRequest, EbMSMessageContext messageContext)
	{
		if (messageContext == null || EbMSAction.EBMS_SERVICE_URI.equals(messageContext.getService()))
			return new Pair<EbMSMessageStatus,Instant>(EbMSMessageStatus.NOT_RECOGNIZED,null);
		else if (!messageContext.getCpaId().equals(statusRequest.getMessageHeader().getCPAId()))
			return new Pair<EbMSMessageStatus,Instant>(EbMSMessageStatus.UNAUTHORIZED,null);
		else
		{
			return ebMSDAO.getMessageStatus(statusRequest.getStatusRequest().getRefToMessageId())
					.map(s -> mapEbMSMessageStatusAndTimestamp(s,messageContext.getTimestamp()))
					.get();
		}
	}

	private Pair<EbMSMessageStatus,Instant> mapEbMSMessageStatusAndTimestamp(EbMSMessageStatus status, Instant timestamp)
	{
		if (status != null
				&& (MessageStatusType.RECEIVED.equals(status.getStatusCode())
						|| MessageStatusType.PROCESSED.equals(status.getStatusCode())
						|| MessageStatusType.FORWARDED.equals(status.getStatusCode())))
			return new Pair<EbMSMessageStatus,Instant>(status,timestamp);
		else
			return new Pair<EbMSMessageStatus,Instant>(EbMSMessageStatus.NOT_RECOGNIZED,null);
	}
}
