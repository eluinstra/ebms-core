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

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActorType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.EncryptionAlgorithm;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessageOrderSemanticsType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PerMessageCharacteristicsType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Transport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSErrorCode;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.SecurityUtils;
import nl.clockwork.ebms.util.StreamUtils;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
public class CPAValidator
{
	@NonNull
	CPAManager cpaManager;

	public void validate(EbMSMessage message) throws EbMSValidationException
	{
		if (!cpaManager.isValid(message.getMessageHeader().getCPAId(),message.getMessageHeader().getMessageData().getTimestamp()))
			throw new EbMSValidationException(EbMSMessageUtils.createError("//Header/MessageHeader/@cpaid",EbMSErrorCode.INCONSISTENT,"Invalid CPA."));
	}

	public void validate(String cpaId) throws ValidatorException
	{
		validate(cpaManager.getCPA(cpaId).orElseThrow(() -> StreamUtils.illegalStateException("CPA",cpaId)));
	}

	public void validate(CollaborationProtocolAgreement cpa) throws ValidatorException
	{
		if (!"2_0b".equals(cpa.getVersion()))
			log.warn("CPA version " + cpa.getVersion() + " detected! CPA version 2_0b expected.");
		if (StatusValueType.PROPOSED.equals(cpa.getStatus().getValue()))
			throw new ValidationException("CPA Status is proposed!");
		if (!cpa.getStart().isBefore(cpa.getEnd()))
			throw new ValidationException("CPA Start date not before End date!");
		if (!Instant.now().isBefore(cpa.getEnd()))
			throw new ValidationException("CPA expired on " + cpa.getEnd());
		if (cpa.getConversationConstraints() != null)
			log.warn("CPA Conversation Constraints not implemented!");
		if (cpa.getSignature() != null)
			log.warn("CPA Signature not implemented!");
		if (cpa.getPackaging() != null && cpa.getPackaging().size() > 0)
			log.warn("Packaging not implemented!");
		validateActions(cpa);
		validateChannels(cpa);
		validateTransports(cpa);
	}

	private void validateActions(CollaborationProtocolAgreement cpa)
	{
		cpa.getPartyInfo().forEach(partyInfo ->
			partyInfo.getCollaborationRole().forEach(role ->
			{
				role.getServiceBinding().getCanSend().forEach(canSend -> validateCanSend(role,canSend));
				role.getServiceBinding().getCanReceive().forEach(canReceive -> validateCanReceive(role,canReceive));
			})
		);
	}

	private void validateCanSend(CollaborationRole role, CanSend canSend)
	{
		if (canSend.getCanReceive() != null && canSend.getCanReceive().size() > 0)
			log.warn("Nesting of actions under CanSend in Service " + CPAUtils.toString(role.getServiceBinding().getService()) +  " not supported!");
		if (canSend.getThisPartyActionBinding().getChannelId().size() > 1)
			log.warn("Multiple channels per action as defined in Action " + canSend.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " not supported! Using first channel.");
		if (canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationReceiptRequired()
				|| canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsIntelligibleCheckRequired()
				|| canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getTimeToAcknowledgeReceipt() != null
				|| canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getTimeToAcknowledgeAcceptance() != null
				|| canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getRetryCount() != null)
			log.warn("Business signals defined in Action " + canSend.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " not supported!");
		//if (canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsAuthorizationRequired())
			//log.warn("Authorization Required defined in Action " + canSend.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " ignored!");
	}

	private void validateCanReceive(CollaborationRole role, CanReceive canReceive)
	{
		if (canReceive.getCanSend() != null && canReceive.getCanSend().size() > 0)
			log.warn("Nesting of actions under CanReceive in Service " + CPAUtils.toString(role.getServiceBinding().getService()) +  " not supported!");
		if (canReceive.getThisPartyActionBinding().getChannelId().size() > 1)
			log.warn("Multiple channels per action as defined in Action " + canReceive.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " not supported! Using first channel.");
		if (canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationReceiptRequired()
				|| canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsIntelligibleCheckRequired()
				|| canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getTimeToAcknowledgeReceipt() != null
				|| canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getTimeToAcknowledgeAcceptance() != null
				|| canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getRetryCount() != null)
			log.warn("Business signals defined in Action " + canReceive.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " not supported!");
		//if (canReceive.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsAuthorizationRequired())
			//log.warn("Authorization Required defined in Action " + canReceive.getThisPartyActionBinding().getAction() + " of Service " + CPAUtils.toString(role.getServiceBinding().getService()) + " ignored!");
	}

	private void validateChannels(CollaborationProtocolAgreement cpa)
	{
		cpa.getPartyInfo().stream().flatMap(p -> p.getDeliveryChannel().stream()).forEach(c -> validateChannel(c));
	}

	private void validateChannel(DeliveryChannel channel)
	{
		if (((DocExchange)channel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging() != null
				&& MessageOrderSemanticsType.GUARANTEED.equals(((DocExchange)channel.getDocExchangeId()).getEbXMLSenderBinding().getReliableMessaging().getMessageOrderSemantics()))
			log.warn("Message Order as defined in DocExchange " + ((DocExchange)channel.getDocExchangeId()).getDocExchangeId() + " not implemented!");
		if (SyncReplyModeType.SIGNALS_ONLY.equals(channel.getMessagingCharacteristics().getSyncReplyMode())
				|| SyncReplyModeType.SIGNALS_AND_RESPONSE.equals(channel.getMessagingCharacteristics().getSyncReplyMode()))
			log.warn("Business signals defined in Channel " + channel.getChannelId() + " not supported!");
		if (PerMessageCharacteristicsType.NEVER.equals(channel.getMessagingCharacteristics().getDuplicateElimination()))
			log.warn("Duplicate Elimination defined in Channel " + channel.getChannelId() + " always enabled!");
		if (ActorType.URN_OASIS_NAMES_TC_EBXML_MSG_ACTOR_NEXT_MSH.equals(channel.getMessagingCharacteristics().getActor()))
			log.warn("Actor NextMSH not supported!");
		if (((DocExchange)channel.getDocExchangeId()).getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null)
		{
			if (((DocExchange)channel.getDocExchangeId()).getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getDigitalEnvelopeProtocol() != null
					&& !"XMLENC".equals(((DocExchange)channel.getDocExchangeId()).getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getDigitalEnvelopeProtocol().getValue()))
				log.warn("Digital Envelope Protocol" + ((DocExchange)channel.getDocExchangeId()).getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getDigitalEnvelopeProtocol().getValue() + " not supported!");
			val encryptionAlgorithm = getEncryptionAlgorithm(((DocExchange)channel.getDocExchangeId()).getEbXMLReceiverBinding().getReceiverDigitalEnvelope().getEncryptionAlgorithm());
			if (encryptionAlgorithm != null)
				try
				{
					if (SecurityUtils.generateKey(encryptionAlgorithm) == null)
						log.warn("Encryption Algorithm " + encryptionAlgorithm + " not supported!");
				}
				catch (NoSuchAlgorithmException e)
				{
					log.warn("Encryption Algorithm " + encryptionAlgorithm + " not supported!",e);
				}
		}
	}

	private String getEncryptionAlgorithm(List<EncryptionAlgorithm> encryptionAlgorithm)
	{
		if (encryptionAlgorithm != null && encryptionAlgorithm.size() > 0)
			return encryptionAlgorithm.get(0).getW3C() == null ? encryptionAlgorithm.get(0).getValue() : encryptionAlgorithm.get(0).getW3C();
		else
			return null;
	}

	private void validateTransports(CollaborationProtocolAgreement cpa)
	{
		cpa.getPartyInfo().stream().flatMap(p -> p.getTransport().stream()).forEach(t -> validateTransport(t));
	}

	private void validateTransport(Transport t)
	{
		if (!"HTTP".equals(t.getTransportSender().getTransportProtocol().getValue()))
			log.warn("Transport protocol " + t.getTransportSender().getTransportProtocol().getValue() + " defined in TransportSender of Transport " + t.getTransportId() + " not implemented!");
		if (!"HTTP".equals(t.getTransportReceiver().getTransportProtocol().getValue()))
			log.warn("Transport protocol " + t.getTransportReceiver().getTransportProtocol().getValue() + " defined in TransportReceiver of Transport " + t.getTransportId() + " not implemented!");
		if (t.getTransportReceiver().getEndpoint().size() > 1)
			log.warn("Multiple endpoints defined in TransportReceiver of Transport " + t.getTransportId() + "not supported! Only allPurpose endpoint supported. Using first endpoint.");
			return;
	}
}
