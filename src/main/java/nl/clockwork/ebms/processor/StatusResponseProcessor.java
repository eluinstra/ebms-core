/*
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


import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.time.Instant;
import java.util.Optional;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.delivery.DeliveryManager;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.EbMSStatusResponse;
import nl.clockwork.ebms.validation.EbMSMessageValidator;
import nl.clockwork.ebms.validation.ValidatorException;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

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

	public EbMSStatusResponse createStatusResponse(final EbMSStatusRequest statusRequest, final Instant timestamp)
			throws ValidatorException, DatatypeConfigurationException, JAXBException, EbMSProcessorException
	{
		val p = ebMSDAO.getEbMSMessageProperties(statusRequest.getStatusRequest().getRefToMessageId()).orElse(null);
		val result = createEbMSMessageStatusAndTimestamp(statusRequest, p);
		return ebMSMessageFactory.createEbMSStatusResponse(statusRequest, result._1, result._2);
	}

	public void sendStatusResponse(final nl.clockwork.ebms.model.EbMSStatusResponse statusResponse)
	{
		val messageHeader = statusResponse.getMessageHeader();
		val uri = cpaManager.getReceivingUri(
				messageHeader.getCPAId(),
				messageHeader.getTo().getPartyId(),
				messageHeader.getTo().getRole(),
				CPAUtils.toString(messageHeader.getService()),
				messageHeader.getAction());
		deliveryManager.sendResponseMessage(uri, statusResponse);
	}

	public void processStatusResponse(Instant timestamp, EbMSStatusResponse statusResponse)
	{
		try
		{
			messageValidator.validate(statusResponse, timestamp);
			deliveryManager.handleResponseMessage(statusResponse);
		}
		catch (ValidatorException e)
		{
			log.warn("Unable to process StatusResponse " + statusResponse.getMessageHeader().getMessageData().getMessageId(), e);
		}
	}

	private Tuple2<EbMSMessageStatus, Instant> createEbMSMessageStatusAndTimestamp(EbMSStatusRequest statusRequest, EbMSMessageProperties messageProperties)
	{
		if (messageProperties == null || EbMSAction.EBMS_SERVICE_URI.equals(messageProperties.getService()))
			return Tuple.of(EbMSMessageStatus.NOT_RECOGNIZED, null);
		else if (!messageProperties.getCpaId().equals(statusRequest.getMessageHeader().getCPAId()))
			return Tuple.of(EbMSMessageStatus.UNAUTHORIZED, null);
		else
		{
			return Optional.ofNullable(messageProperties.getMessageStatus())
					.map(s -> mapEbMSMessageStatusAndTimestamp(s, messageProperties.getTimestamp()))
					.orElseThrow(() -> new EbMSProcessingException("Not found message " + statusRequest.getStatusRequest().getRefToMessageId()));
		}
	}

	private Tuple2<EbMSMessageStatus, Instant> mapEbMSMessageStatusAndTimestamp(EbMSMessageStatus status, Instant timestamp)
	{
		if (status != null
				&& (MessageStatusType.RECEIVED.equals(status.getStatusCode())
						|| MessageStatusType.PROCESSED.equals(status.getStatusCode())
						|| MessageStatusType.FORWARDED.equals(status.getStatusCode())))
			return Tuple.of(status, timestamp);
		else
			return Tuple.of(EbMSMessageStatus.NOT_RECOGNIZED, null);
	}
}
