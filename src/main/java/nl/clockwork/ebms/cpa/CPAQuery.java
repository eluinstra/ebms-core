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
package nl.clockwork.ebms.cpa;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.OverrideMshActionBinding;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PersistenceLevelType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

public class CPAQuery
{

	static Predicate<CollaborationProtocolAgreement> isValidCPA(Instant timestamp)
	{
		return cpa -> StatusValueType.AGREED.equals(cpa.getStatus().getValue()) && timestamp.compareTo(cpa.getStart()) >= 0
				&& timestamp.compareTo(cpa.getEnd()) <= 0;
	}

	private static Predicate<org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId> matchesPartyId(String partyId)
	{
		return p -> CPAUtils.toString(p).equals(partyId);
	}

	private static Predicate<PartyInfo> matchesPartyInfo(List<PartyId> partyId)
	{
		return partyInfo -> CPAUtils.equals(partyInfo.getPartyId(),partyId);
	}

	private static Predicate<PartyInfo> isEmptyOrMatchesPartyInfo(Party fromParty)
	{
		return partyInfo -> fromParty == null || fromParty.matches(partyInfo.getPartyId());
	}

	private static Predicate<CollaborationRole> isEmptyOrMatchesRole(Party fromParty)
	{
		return collaborationRole -> fromParty == null || fromParty.matches(collaborationRole.getRole());
	}

	private static Predicate<CollaborationRole> matchesRoleByRole(String role)
	{
		return collaborationRole -> collaborationRole.getRole().getName().equals(role);
	}

	private static Predicate<CollaborationRole> matchesRoleByService(String service)
	{
		return collaborationRole -> CPAUtils.toString(collaborationRole.getServiceBinding().getService()).equals(service);
	}

	private static Predicate<CanSend> matchesCanSend(String action)
	{
		return canSend -> canSend.getThisPartyActionBinding().getAction().equals(action);
	}

	private static Predicate<CanReceive> matchesCanReceive(String action)
	{
		return canReceive -> canReceive.getThisPartyActionBinding().getAction().equals(action);
	}

	private static Predicate<CanReceive> matchesCanReceive(FromPartyInfo fromPartyInfo)
	{
		return canReceive -> canReceive.getThisPartyActionBinding().equals(fromPartyInfo.getCanSend().getOtherPartyActionBinding());
	}

	private static Predicate<DeliveryChannel> matchesDeliveryChannel(String deliveryChannelId)
	{
		return deliveryChannel -> deliveryChannel.getChannelId().equals(deliveryChannelId);
	}

	private static Predicate<OverrideMshActionBinding> matchesOverrideMshActionBinding(String action)
	{
		return binding -> binding.getAction().equals(action);
	}

	private static Predicate<CanSend> isNonRepudiationRequired(DocExchange docExchange)
	{
		return canSend -> canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired()
				&& docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null;
	}

	private static Predicate<CanSend> isConfidential(DocExchange docExchange)
	{
		return canSend -> (PersistenceLevelType.PERSISTENT.equals(canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential())
				|| PersistenceLevelType.TRANSIENT_AND_PERSISTENT
						.equals(canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()))
				&& docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null;
	}

	private static Function<CollaborationProtocolAgreement,Stream<CollaborationRole>> findRoles(List<PartyId> partyId, String role, String service)
	{
		return cpa -> cpa.getPartyInfo()
				.stream()
				.filter(matchesPartyInfo(partyId))
				.flatMap(streamRoles())
				.filter(matchesRoleByRole(role).and(matchesRoleByService(service)));
	}

	private static Optional<EbMSPartyInfo> findEbMSPartyInfo(String partyId, PartyInfo partyInfo)
	{
		return partyInfo.getPartyId().stream().filter(matchesPartyId(partyId)).map(toEbMSPartyInfo()).findFirst();
	}

	private static Function<PartyInfo,Stream<CollaborationRole>> findRoles(String role, String service)
	{
		return partyInfo -> partyInfo.getCollaborationRole().stream().filter(matchesRoleByRole(role).and(matchesRoleByService(service)));
	}

	private static Function<PartyInfo,Stream<CollaborationRole>> streamRoles()
	{
		return partyInfo -> partyInfo.getCollaborationRole().stream();
	}

	private static Function<CollaborationRole,Stream<CanSend>> streamCanSends()
	{
		return collaborationRole -> collaborationRole.getServiceBinding().getCanSend().stream();
	}

	private static Function<CollaborationRole,Stream<CanReceive>> streamCanReceives()
	{
		return collaborationRole -> collaborationRole.getServiceBinding().getCanReceive().stream();
	}

	private static Function<PartyInfo,Stream<DeliveryChannel>> streamDeliveryChannels()
	{
		return partyInfo -> partyInfo.getDeliveryChannel().stream();
	}

	private static Function<org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId,EbMSPartyInfo> toEbMSPartyInfo()
	{
		return id -> new EbMSPartyInfo(CPAUtils.toPartyId(id));
	}

	private static Function<CanSend,FromPartyInfo> toFromPartyInfo(Party fromParty, PartyInfo partyInfo, CollaborationRole collaborationRole)
	{
		return canSend -> CPAUtils
				.getFromPartyInfo(fromParty == null ? partyInfo.getPartyId().get(0) : fromParty.getPartyId(partyInfo.getPartyId()),collaborationRole,canSend);
	}

	private static Function<CanReceive,ToPartyInfo> toToPartyInfo(PartyInfo partyInfo, CollaborationRole collaborationRole)
	{
		return canReceive -> CPAUtils.getToPartyInfo(partyInfo.getPartyId().get(0),collaborationRole,canReceive);
	}

	private static Function<CanReceive,ToPartyInfo> toToPartyInfo(Party toParty, PartyInfo partyInfo, CollaborationRole collaborationRole)
	{
		return canReceive -> CPAUtils
				.getToPartyInfo(toParty == null ? partyInfo.getPartyId().get(0) : toParty.getPartyId(partyInfo.getPartyId()),collaborationRole,canReceive);
	}

	private static Function<OverrideMshActionBinding,DeliveryChannel> toDeliveryChannel()
	{
		return binding -> (DeliveryChannel)binding.getChannelId();
	}

	private static Function<CanSend,DeliveryChannel> canSendToDeliveryChannel()
	{
		return canSend -> CPAUtils.getDeliveryChannel(canSend.getThisPartyActionBinding().getChannelId());
	}

	private static Function<CanReceive,DeliveryChannel> canReceiveToDeliveryChannel()
	{
		return canReceive -> CPAUtils.getDeliveryChannel(canReceive.getThisPartyActionBinding().getChannelId());
	}

	static Function<CollaborationProtocolAgreement,Boolean> existsPartyId(String partyId)
	{
		return cpa -> cpa.getPartyInfo().stream().flatMap(partyInfo -> partyInfo.getPartyId().stream()).anyMatch(matchesPartyId(partyId));
	}

	static Function<CollaborationProtocolAgreement,Optional<EbMSPartyInfo>> getEbMSPartyInfo(String partyId)
	{
		return cpa -> cpa.getPartyInfo()
				.stream()
				.filter(partyInfo -> partyInfo.getPartyId().stream().anyMatch(matchesPartyId(partyId)))
				.map(partyInfo -> findEbMSPartyInfo(partyId,partyInfo).orElseThrow(() -> new IllegalStateException("PartyId " + partyId + " not found")))
				.findFirst();
	}

	static Function<CollaborationProtocolAgreement,Optional<PartyInfo>> getPartyInfo(List<PartyId> partyId)
	{
		return cpa -> cpa.getPartyInfo().stream().filter(matchesPartyInfo(partyId)).findFirst();
	}

	static Function<CollaborationProtocolAgreement,Optional<FromPartyInfo>> getFromPartyInfo(Party fromParty, String service, String action)
	{
		return cpa -> cpa.getPartyInfo()
				.stream()
				.filter(isEmptyOrMatchesPartyInfo(fromParty))
				.flatMap(partyInfo -> partyInfo.getCollaborationRole()
						.stream()
						.filter(isEmptyOrMatchesRole(fromParty).and(matchesRoleByService(service)))
						.flatMap(collaborationRole -> collaborationRole.getServiceBinding()
								.getCanSend()
								.stream()
								.filter(matchesCanSend(action))
								.map(toFromPartyInfo(fromParty,partyInfo,collaborationRole))))
				.findFirst();
	}

	static Function<CollaborationProtocolAgreement,Optional<ToPartyInfo>>
			getToPartyInfoByFromPartyActionBinding(FromPartyInfo fromPartyInfo, Party fromParty, String service, String action)
	{
		return cpa -> cpa.getPartyInfo()
				.stream()
				.flatMap(partyInfo -> partyInfo.getCollaborationRole()
						.stream()
						.flatMap(collaborationRole -> collaborationRole.getServiceBinding()
								.getCanReceive()
								.stream()
								.filter(matchesCanReceive(fromPartyInfo))
								.map(toToPartyInfo(partyInfo,collaborationRole))))
				.findFirst();
	}

	static Function<CollaborationProtocolAgreement,Optional<ToPartyInfo>> getToPartyInfo(Party toParty, String service, String action)
	{
		return cpa -> cpa.getPartyInfo()
				.stream()
				.filter(isEmptyOrMatchesPartyInfo(toParty))
				.flatMap(partyInfo -> partyInfo.getCollaborationRole()
						.stream()
						.filter(isEmptyOrMatchesRole(toParty).and(matchesRoleByService(service)))
						.flatMap(collaborationRole -> collaborationRole.getServiceBinding()
								.getCanReceive()
								.stream()
								.filter(matchesCanReceive(action))
								.map(toToPartyInfo(toParty,partyInfo,collaborationRole))))
				.findFirst();
	}

	static Function<CollaborationProtocolAgreement,Boolean> canSend(List<PartyId> partyId, String role, String service, String action)
	{
		return cpa -> findRoles(partyId,role,service).apply(cpa).flatMap(streamCanSends()).anyMatch(matchesCanSend(action));
	}

	static Function<CollaborationProtocolAgreement,Boolean> canReceive(List<PartyId> partyId, String role, String service, String action)
	{
		return cpa -> findRoles(partyId,role,service).apply(cpa).flatMap(streamCanReceives()).anyMatch(matchesCanReceive(action));
	}

	static Function<CollaborationProtocolAgreement,Optional<DeliveryChannel>> getDeliveryChannel(String deliveryChannelId)
	{
		return cpa -> cpa.getPartyInfo().stream().flatMap(streamDeliveryChannels()).filter(matchesDeliveryChannel(deliveryChannelId)).findFirst();
	}

	static Function<PartyInfo,Optional<DeliveryChannel>> getDefaultDeliveryChannel(String action)
	{
		return partyInfo -> partyInfo.getOverrideMshActionBinding()
				.stream()
				.filter(matchesOverrideMshActionBinding(action))
				.map(toDeliveryChannel())
				.findFirst()
				.or(() -> Optional.ofNullable((DeliveryChannel)partyInfo.getDefaultMshChannelId()));
	}

	static Function<PartyInfo,Optional<DeliveryChannel>> getSendDeliveryChannel(String role, String service, String action)
	{
		return partyInfo -> findRoles(role,service).apply(partyInfo)
				.flatMap(streamCanSends())
				.filter(matchesCanSend(action))
				.map(canSendToDeliveryChannel())
				.findFirst();
	}

	static Function<PartyInfo,Optional<DeliveryChannel>> getReceiveDeliveryChannel(String role, String service, String action)
	{
		return partyInfo -> findRoles(role,service).apply(partyInfo)
				.flatMap(streamCanReceives())
				.filter(matchesCanReceive(action))
				.map(canReceiveToDeliveryChannel())
				.findFirst();
	}

	static Function<CollaborationProtocolAgreement,Boolean>
			isSendingNonRepudiationRequired(DocExchange docExchange, List<PartyId> partyId, String role, String service, String action)
	{
		return cpa -> findRoles(partyId,role,service).apply(cpa)
				.flatMap(streamCanSends())
				.filter(matchesCanSend(action))
				.anyMatch(isNonRepudiationRequired(docExchange));
	}

	static Function<CollaborationProtocolAgreement,Boolean>
			isSendingConfidential(DocExchange docExchange, List<PartyId> partyId, String role, String service, String action)
	{
		return cpa -> findRoles(partyId,role,service).apply(cpa).flatMap(streamCanSends()).filter(matchesCanSend(action)).anyMatch(isConfidential(docExchange));
	}

}
