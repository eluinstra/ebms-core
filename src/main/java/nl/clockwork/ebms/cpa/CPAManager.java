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

import static java.util.Optional.empty;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.MessagingCharacteristics;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.springframework.cache.annotation.CacheEvict;
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
public class CPAManager implements WithCPA
{
	@NonNull
	CPADAO cpaDAO;
	@NonNull
	URLMapper urlMapper;
	Object cpaMonitor = new Object();

	@CacheEvict(cacheNames = "CPA", allEntries = true)
	public void clearCache()
	{
		//do nothing
	}

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

	public int deleteCPA(String cpaId)
	{
		return cpaDAO.deleteCPA(cpaId);
	}

	public boolean isValid(String cpaId, Instant timestamp)
	{
		return getCPA(cpaId).filter(isValidCPA(timestamp)).isPresent();
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public boolean existsPartyId(String cpaId, String partyId)
	{
		return getCPA(cpaId).map(existsPartyId(partyId)).orElse(false);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, String partyId)
	{
		return getCPA(cpaId).map(getEbMSPartyInfo(partyId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)")
	public Optional<PartyInfo> getPartyInfo(String cpaId, List<PartyId> partyId)
	{
		return getCPA(cpaId).map(getPartyInfo(partyId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Party fromParty, String service, String action)
	{
		return getCPA(cpaId).map(getFromPartyInfo(fromParty,service,action)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Party fromParty, String service, String action)
	{
		return getFromPartyInfo(cpaId,fromParty,service,action)
				.flatMap(fromPartyInfo -> getCPA(cpaId)
						.map(getToPartyInfoByFromPartyActionBinding(fromPartyInfo,fromParty,service,action)))
				.orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<ToPartyInfo> getToPartyInfo(String cpaId, Party toParty, String service, String action)
	{
		return getCPA(cpaId).map(getToPartyInfo(toParty,service,action)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canSend(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId).map(canSend(partyId,role,service,action)).orElse(false);
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean canReceive(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return getCPA(cpaId).map(canReceive(partyId,role,service,action)).orElse(false);
	}

	@Cacheable(cacheNames = "CPA", keyGenerator = "ebMSKeyGenerator")
	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return getCPA(cpaId).map(getDeliveryChannel(deliveryChannelId)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#action")
	public Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, List<PartyId> partyId, String action)
	{
		return getPartyInfo(cpaId,partyId).map(getDefaultDeliveryChannel(action)).orElse(empty());
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return EbMSAction.EBMS_SERVICE_URI.equals(service)
				? getDefaultDeliveryChannel(cpaId,partyId,action)
				: getPartyInfo(cpaId,partyId)
							.flatMap(getSendDeliveryChannel(role, service, action));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		return EbMSAction.EBMS_SERVICE_URI.equals(service)
				? getDefaultDeliveryChannel(cpaId,partyId,action)
				: getPartyInfo(cpaId,partyId)
							.flatMap(getReceiveDeliveryChannel(role, service, action));
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isSendingNonRepudiationRequired(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
						.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return getCPA(cpaId)
				.map(isSendingNonRepudiationRequired(docExchange, partyId, role, service, action))
				.orElse(false);
	}

	@Cacheable(cacheNames = "CPA", key = "#root.methodName+#cpaId+T(nl.clockwork.ebms.cpa.CPAUtils).toString(#partyId)+#role+#service+#action")
	public boolean isSendingConfidential(String cpaId, List<PartyId> partyId, String role, String service, String action)
	{
		val docExchange = CPAUtils.getDocExchange(
				getSendDeliveryChannel(cpaId,partyId,role,service,action)
						.orElseThrow(() -> StreamUtils.illegalStateException("SendDeliveryChannel",cpaId,partyId,role,service,action)));
		return getCPA(cpaId)
				.map(isSendingConfidential(docExchange, partyId, role, service, action))
				.orElse(false);
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
