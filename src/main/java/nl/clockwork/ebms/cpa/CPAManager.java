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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
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
		val cpa = getCPA(cpaId);
		return cpa
				.map(c -> StatusValueType.AGREED.equals(c.getStatus().getValue())
						&& timestamp.compareTo(c.getStart()) >= 0
						&& timestamp.compareTo(c.getEnd()) <= 0)
				.orElse(false);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsPartyId(String cpaId, String partyId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.flatMap(p -> p.getPartyId().stream())
						.anyMatch(id -> partyId.equals(CPAUtils.toString(id))))
				.orElse(false);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, String partyId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> p.getPartyId().stream()
								.anyMatch(id -> partyId.equals(CPAUtils.toString(id))))
						.map(p -> createEbMSPartyInfo(partyId,p))
						.findFirst()
						.orElse(null));
	}

	private EbMSPartyInfo createEbMSPartyInfo(String partyId, PartyInfo partyInfo)
	{
		return partyInfo.getPartyId().stream()
				.filter(id -> partyId.equals(CPAUtils.toString(id)))
				.findFirst()
				.map(id -> new EbMSPartyInfo(CPAUtils.toPartyId(id)))
				.orElseThrow(() -> new IllegalStateException("PartyId " + partyId + " not found"));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)")
	public Optional<PartyInfo> getPartyInfo(String cpaId, List<PartyId> partyId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.findFirst()
						.orElse(null));
	}
	
	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Party fromParty, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> fromParty == null || fromParty.matches(p.getPartyId()))
						.flatMap(p -> p.getCollaborationRole().stream()
								.filter(r -> fromParty == null || fromParty.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
								.flatMap(r -> r.getServiceBinding().getCanSend().stream()
										.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
										.map(cs -> CPAUtils.getFromPartyInfo(fromParty == null ? p.getPartyId().get(0) : fromParty.getPartyId(p.getPartyId()),r,cs))))
						.findFirst()
						.orElse(null));
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
	{
		return getFromPartyInfo(cpaId,fromParty,service,action)
				.map(fpi -> getCPA(cpaId)
					.map(c -> c.getPartyInfo().stream()
							.flatMap(p -> p.getCollaborationRole().stream()
									.flatMap(r -> r.getServiceBinding().getCanReceive().stream()
											.filter(cr -> cr.getThisPartyActionBinding().equals(fpi.getCanSend().getOtherPartyActionBinding()))
											.map(cr -> CPAUtils.getToPartyInfo(p.getPartyId().get(0),r,cr))))
							.findFirst()
							.orElse(null))
					.orElse(null));
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfo(String cpaId, Party toParty, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> toParty == null || toParty.matches(p.getPartyId()))
						.flatMap(p -> p.getCollaborationRole().stream()
								.filter(r -> toParty == null || toParty.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
								.flatMap(r -> r.getServiceBinding().getCanReceive().stream()
									.filter(cr -> action.equals(cr.getThisPartyActionBinding().getAction()))
									.map(cr -> CPAUtils.getToPartyInfo(toParty == null ? p.getPartyId().get(0) : toParty.getPartyId(p.getPartyId()),r,cr))))
						.findFirst()
						.orElse(null));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canSend(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.flatMap(p -> p.getCollaborationRole().stream())
						.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
						.flatMap(r -> r.getServiceBinding().getCanSend().stream())
						.anyMatch(cs -> action.equals(cs.getThisPartyActionBinding().getAction())))
				.orElse(null);
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canReceive(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.flatMap(p -> p.getCollaborationRole().stream())
						.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
						.flatMap(r -> r.getServiceBinding().getCanReceive().stream())
						.anyMatch(cr -> action.equals(cr.getThisPartyActionBinding().getAction())))
				.orElse(null);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
					.flatMap(p -> p.getDeliveryChannel().stream())
					.filter(d -> d.getChannelId().equals(deliveryChannelId))
					.findFirst()
					.orElse(null));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#action")
	public Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action)
	{
		return getPartyInfo(cpaId,partyId)
				.map(p -> p.getOverrideMshActionBinding().stream()
					.filter(b -> b.getAction().equals(action))
					.findFirst()
					.map(b -> (DeliveryChannel)b.getChannelId())
					.orElse((DeliveryChannel)p.getDefaultMshChannelId()));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		if (EbMSAction.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
		{
			return getPartyInfo(cpaId,partyId)
					.map(p -> p.getCollaborationRole().stream()
							.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
							.flatMap(r -> r.getServiceBinding().getCanSend().stream())
							.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
							.findFirst()
							.map(cs -> CPAUtils.getDeliveryChannel(cs.getThisPartyActionBinding().getChannelId()))
							.orElse(null));
		}
	}
	
	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		if (EbMSAction.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
			return getPartyInfo(cpaId,partyId)
					.map(p -> p.getCollaborationRole().stream()
							.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
							.flatMap(r -> r.getServiceBinding().getCanReceive().stream())
							.filter(cr -> action.equals(cr.getThisPartyActionBinding().getAction()))
							.findFirst()
							.map(cr -> CPAUtils.getDeliveryChannel(cr.getThisPartyActionBinding().getChannelId()))
							.orElse(null));
	}
	
	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val canSend =  getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.flatMap(p -> p.getCollaborationRole().stream())
						.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
						.flatMap(r -> r.getServiceBinding().getCanSend().stream())
						.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
						.findFirst()
						.orElse(null));
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
					.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return canSend
				.map(cs -> cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired()
					&& docExchange.getEbXMLSenderBinding() != null
					&& docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null)
				.orElse(null);
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isConfidential(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val canSend =  getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanSend().stream())
				.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
				.findFirst()
				.orElse(null));
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
					.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return canSend.map(cs ->(PersistenceLevelType.PERSISTENT.equals(cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()) 
				|| PersistenceLevelType.TRANSIENT_AND_PERSISTENT.equals(cs.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()))
				&& docExchange.getEbXMLReceiverBinding() != null
				&& docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null)
				.orElse(false);
	}

	public String getUri(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return urlMapper.getURL(CPAUtils.getUri(
				getReceiveDeliveryChannel(cpaId,partyId,role,service,action)
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",cpaId,partyId,role,service,action))
		));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<SyncReplyModeType> getSyncReply(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getSendDeliveryChannel(cpaId,partyId,role,service,action)
				.map(c -> c.getMessagingCharacteristics().getSyncReplyMode());
	}
}
