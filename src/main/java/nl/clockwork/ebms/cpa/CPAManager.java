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
package nl.clockwork.ebms.cpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessagingCharacteristics;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.OverrideMshActionBinding;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PersistenceLevelType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.springframework.cache.annotation.Cacheable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.cpa.url.URLMapper;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CPAManager
{
	private static Predicate<CollaborationProtocolAgreement> isValidCPA(Instant timestamp)
	{
		return cpa -> StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart()) >= 0
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

	private static Predicate<CollaborationRole> isEmptyOrMatchesRole(Party toParty, String service)
	{
		return collaborationRole -> toParty == null || toParty.matches(collaborationRole.getRole()) && service.equals(CPAUtils.toString(collaborationRole.getServiceBinding().getService()));
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

	private Predicate<CanSend> isNonRepudiationRequired(final org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange docExchange)
	{
		return cs -> cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired()
				&& docExchange.getEbXMLSenderBinding() != null
				&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null;
	}
	private Predicate<CanSend> isConfidential(final org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange docExchange)
	{
		return cs ->(PersistenceLevelType.PERSISTENT.equals(cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()) 
				|| PersistenceLevelType.TRANSIENT_AND_PERSISTENT.equals(cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()))
				&& docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null;
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
		return canSend -> CPAUtils.getFromPartyInfo(fromParty == null ? partyInfo.getPartyId().get(0) : fromParty.getPartyId(partyInfo.getPartyId()),collaborationRole,canSend);
	}
	private static Function<CanReceive,ToPartyInfo> toToPartyInfo(PartyInfo partyInfo, CollaborationRole collaborationRole)
	{
		return canReceive -> CPAUtils.getToPartyInfo(partyInfo.getPartyId().get(0),collaborationRole,canReceive);
	}
	private static Function<CanReceive,ToPartyInfo> toToPartyInfo(Party toParty, PartyInfo partyInfo, CollaborationRole collaborationRole)
	{
		return canReceive -> CPAUtils.getToPartyInfo(toParty == null ? partyInfo.getPartyId().get(0) : toParty.getPartyId(partyInfo.getPartyId()),collaborationRole,canReceive);
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

	@NonNull
	CPADAO cpaDAO;
	@NonNull
	URLMapper urlMapper;
	Object cpaMonitor = new Object();

	public boolean existsCPA(String cpaId)
	{
		return cpaDAO.existsCPA(cpaId);
	}

	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		return cpaDAO.getCPA(cpaId);
	}

	public List<String> getCPAIds()
	{
		return cpaDAO.getCPAIds();
	}

	public void setCPA(CollaborationProtocolAgreement cpa, Boolean overwrite)
	{
		synchronized (cpaMonitor)
		{
			if (cpaDAO.existsCPA(cpa.getCpaid()))
			{
				if (overwrite != null && overwrite)
				{
					if (cpaDAO.updateCPA(cpa) == 0)
						throw new IllegalArgumentException("Could not update CPA " + cpa.getCpaid() + "! CPA does not exists.");
				}
				else
					throw new IllegalArgumentException("Did not insert CPA " + cpa.getCpaid() + "! CPA already exists.");
			}
			else
				cpaDAO.insertCPA(cpa);
		}
	}

	public long deleteCPA(String cpaId)
	{
		return cpaDAO.deleteCPA(cpaId);
	}

	public boolean isValid(String cpaId, Instant timestamp)
	{
		return getCPA(cpaId)
				.filter(isValidCPA(timestamp))
				.isPresent();
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsPartyId(String cpaId, String partyId)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.flatMap(partyInfo -> partyInfo.getPartyId().stream())
						.filter(matchesPartyId(partyId))
						.findAny())
				.isPresent();
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, String partyId)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(partyInfo -> partyInfo.getPartyId().stream()
								.anyMatch(matchesPartyId(partyId)))
						.map(partyInfo -> createEbMSPartyInfo(partyId,partyInfo)
								.orElseThrow(() -> new IllegalStateException("PartyId " + partyId + " not found")))
						.findFirst());
	}

	private Optional<EbMSPartyInfo> createEbMSPartyInfo(String partyId, PartyInfo partyInfo)
	{
		return partyInfo.getPartyId().stream()
				.filter(matchesPartyId(partyId))
				.map(toEbMSPartyInfo())
				.findFirst();
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)")
	public Optional<PartyInfo> getPartyInfo(String cpaId, List<PartyId> partyId)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(matchesPartyInfo(partyId))
						.findFirst());
	}
	
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Party fromParty, String service, String action)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(isEmptyOrMatchesPartyInfo(fromParty))
						.flatMap(partyInfo -> partyInfo.getCollaborationRole().stream()
								.filter(isEmptyOrMatchesRole(fromParty).and(matchesRoleByService(service)))
								.flatMap(collaborationRole -> collaborationRole.getServiceBinding().getCanSend().stream()
										.filter(matchesCanSend(action))
										.map(toFromPartyInfo(fromParty,partyInfo,collaborationRole))))
						.findFirst());
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
	{
		return getFromPartyInfo(cpaId,fromParty,service,action)
				.flatMap(fromPartyInfo -> getCPA(cpaId)
						.flatMap(cpa -> cpa.getPartyInfo().stream()
								.flatMap(partyInfo -> partyInfo.getCollaborationRole().stream()
										.flatMap(collaborationRole -> collaborationRole.getServiceBinding().getCanReceive().stream()
												.filter(matchesCanReceive(fromPartyInfo))
												.map(toToPartyInfo(partyInfo,collaborationRole))))
								.findFirst()));
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfo(String cpaId, Party toParty, String service, String action)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(isEmptyOrMatchesPartyInfo(toParty))
						.flatMap(partyInfo -> partyInfo.getCollaborationRole().stream()
								.filter(isEmptyOrMatchesRole(toParty,service))
								.flatMap(collaborationRole -> collaborationRole.getServiceBinding().getCanReceive().stream()
										.filter(matchesCanReceive(action))
										.map(toToPartyInfo(toParty,partyInfo,collaborationRole))))
						.findFirst());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canSend(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(matchesPartyInfo(partyId))
						.flatMap(streamRoles())
						.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
						.flatMap(streamCanSends())
						.filter(matchesCanSend(action))
						.findAny())
				.isPresent();
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canReceive(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(matchesPartyInfo(partyId))
						.flatMap(streamRoles())
						.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
						.flatMap(streamCanReceives())
						.filter(matchesCanReceive(action))
						.findAny())
				.isPresent();
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.flatMap(streamDeliveryChannels())
						.filter(matchesDeliveryChannel(deliveryChannelId))
						.findFirst());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#action")
	public Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action)
	{
		return getPartyInfo(cpaId,partyId)
				.map(partyInfo -> partyInfo.getOverrideMshActionBinding().stream()
						.filter(matchesOverrideMshActionBinding(action))
						.map(toDeliveryChannel())
						.findFirst()
						.orElse((DeliveryChannel)partyInfo.getDefaultMshChannelId()));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return EbMSAction.EBMS_SERVICE_URI.equals(service)
				? getDefaultDeliveryChannel(cpaId,partyId,action)
				: getPartyInfo(cpaId,partyId)
							.flatMap(partyInfo -> partyInfo.getCollaborationRole().stream()
									.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
									.flatMap(streamCanSends())
									.filter(matchesCanSend(action))
									.map(canSendToDeliveryChannel())
									.findFirst());
	}
	
	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return EbMSAction.EBMS_SERVICE_URI.equals(service)
				? getDefaultDeliveryChannel(cpaId,partyId,action)
				: getPartyInfo(cpaId,partyId)
							.flatMap(partyInfo -> partyInfo.getCollaborationRole().stream()
									.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
									.flatMap(streamCanReceives())
									.filter(matchesCanReceive(action))
									.map(canReceiveToDeliveryChannel())
									.findFirst());
	}
	
	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isSendingNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
						.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return getCPA(cpaId)
				.flatMap(cpa -> cpa.getPartyInfo().stream()
						.filter(matchesPartyInfo(partyId))
						.flatMap(streamRoles())
						.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
						.flatMap(streamCanSends())
						.filter(matchesCanSend(action))
						.findAny())
				.filter(isNonRepudiationRequired(docExchange))
				.isPresent();
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isSendingConfidential(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
						.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return getCPA(cpaId)
				.flatMap(c -> c.getPartyInfo().stream()
						.filter(matchesPartyInfo(partyId))
						.flatMap(streamRoles())
						.filter(matchesRoleByRole(role).and(matchesRoleByService(service)))
						.flatMap(streamCanSends())
						.filter(matchesCanSend(action))
						.findAny())
				.filter(isConfidential(docExchange))
				.isPresent();
	}

	public String getReceivingUri(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return urlMapper.getURL(CPAUtils.getUri(
				getReceiveDeliveryChannel(cpaId,partyId,role,service,action)
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",cpaId,partyId,role,service,action))
		));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<SyncReplyModeType> getSendSyncReply(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getSendDeliveryChannel(cpaId,partyId,role,service,action)
				.map(DeliveryChannel::getMessagingCharacteristics)
				.map(MessagingCharacteristics::getSyncReplyMode);
	}
}
