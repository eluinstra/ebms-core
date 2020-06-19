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

import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PersistenceLevelType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.cache.CachingMethodInterceptor;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.util.StreamUtils;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class CPAManager
{
	@NonNull
	CPADAO cpaDAO;
	@NonNull
	URLMapper urlMapper;
//	@NonNull
//	CachingMethodInterceptor cpaMethodCache;

	@CacheResult(cacheName = "existsCPA")
	public boolean existsCPA(String cpaId)
	{
		return cpaDAO.existsCPA(cpaId);
	}

	@CacheResult(cacheName = "CPA")
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		return cpaDAO.getCPA(cpaId);
	}

	@Cacheable(cacheNames = "CPAIds")
	public List<String> getCPAIds()
	{
		return cpaDAO.getCPAIds();
	}

	@CacheEvict(cacheNames = {"existsCPA","CPA","CPAIds"}, allEntries = true)
	public void insertCPA(CollaborationProtocolAgreement cpa)
	{
		cpaDAO.insertCPA(cpa);
		flushCPAMethodCache(cpa.getCpaid());
	}

	public long updateCPA(CollaborationProtocolAgreement cpa)
	{
		val result = cpaDAO.updateCPA(cpa);
		flushCPAMethodCache(cpa.getCpaid());
		return result;
	}

	public long deleteCPA(String cpaId)
	{
		val result = cpaDAO.deleteCPA(cpaId);
		flushCPAMethodCache(cpaId);
		return result;
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

	public boolean existsPartyId(String cpaId, String partyId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.flatMap(p -> p.getPartyId().stream())
						.anyMatch(id -> partyId.equals(CPAUtils.toString(id))))
				.orElse(false);
	}

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
		val result = new EbMSPartyInfo();
		result.setPartyIds(CPAUtils.toPartyId(partyInfo.getPartyId().stream()
				.filter(id -> partyId.equals(CPAUtils.toString(id))).findFirst().orElse(null)));
		return result;
	}

	public Optional<PartyInfo> getPartyInfo(String cpaId, CacheablePartyId partyId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.findFirst()
						.orElse(null));
	}
	
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

	public boolean canSend(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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

	public boolean canReceive(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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

	public Optional<DeliveryChannel> getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
					.flatMap(p -> p.getDeliveryChannel().stream())
					.filter(d -> d.getChannelId().equals(deliveryChannelId))
					.findFirst()
					.orElse(null));
	}

	public Optional<DeliveryChannel> getDefaultDeliveryChannel(String cpaId, CacheablePartyId partyId, String action)
	{
//		return getCPA(cpaId)
//				.map(c -> c.getPartyInfo().stream()
//					.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
//					.flatMap(p -> p.getOverrideMshActionBinding().stream())
//					.filter(b -> b.getAction().equals(action))
//					.findFirst()
//					.map(b -> (DeliveryChannel)b.getChannelId())
//					.orElse(c.getPartyInfo().stream()
//							.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
//							.map(p -> (DeliveryChannel)p.getDefaultMshChannelId())
//							.findFirst()
//							.orElse(null)));
		return getPartyInfo(cpaId,partyId)
				.map(p -> p.getOverrideMshActionBinding().stream()
					.filter(b -> b.getAction().equals(action))
					.findFirst()
					.map(b -> (DeliveryChannel)b.getChannelId())
					.orElse((DeliveryChannel)p.getDefaultMshChannelId()));
	}

	public Optional<DeliveryChannel> getSendDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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
	
	public Optional<DeliveryChannel> getReceiveDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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
	
	public boolean isNonRepudiationRequired(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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

	public boolean isConfidential(String cpaId, CacheablePartyId partyId, String role, String service, String action)
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

	public String getUri(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return urlMapper.getURL(CPAUtils.getUri(
				getReceiveDeliveryChannel(cpaId,partyId,role,service,action)
					.orElseThrow(() -> StreamUtils.illegalStateException("ReceiveDeliveryChannel",cpaId,partyId,role,service,action))
		));
	}

	public Optional<SyncReplyModeType> getSyncReply(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return getSendDeliveryChannel(cpaId,partyId,role,service,action)
				.map(c -> c.getMessagingCharacteristics().getSyncReplyMode());
	}

	private void flushCPAMethodCache(String cpaId)
	{
		//cpaMethodCache.removeAll();
	}
}
