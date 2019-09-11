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
package nl.clockwork.ebms.common;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PersistenceLevelType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.StreamUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.CPAUtils;

public class CPAManager
{
	private Ehcache methodCache;
	private EbMSDAO ebMSDAO;
	private URLManager urlManager;

	public boolean existsCPA(String cpaId)
	{
		return ebMSDAO.existsCPA(cpaId);
	}

	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId)
	{
		return ebMSDAO.getCPA(cpaId);
	}

	public List<String> getCPAIds()
	{
		return ebMSDAO.getCPAIds();
	}

	public void insertCPA(CollaborationProtocolAgreement cpa)
	{
		ebMSDAO.insertCPA(cpa);
		flushCPAMethodCache(cpa.getCpaid());
	}

	public int updateCPA(CollaborationProtocolAgreement cpa)
	{
		int result = ebMSDAO.updateCPA(cpa);
		flushCPAMethodCache(cpa.getCpaid());
		return result;
	}

	public int deleteCPA(String cpaId)
	{
		int result = ebMSDAO.deleteCPA(cpaId);
		flushCPAMethodCache(cpaId);
		return result;
	}

	public boolean isValid(String cpaId, Date timestamp)
	{
		Optional<CollaborationProtocolAgreement> cpa = getCPA(cpaId);
		return cpa
				.map(c -> StatusValueType.AGREED.equals(c.getStatus().getValue())
						&& timestamp.compareTo(c.getStart()) >= 0
						&& timestamp.compareTo(c.getEnd()) <= 0)
				.orElse(false);
	}

	public boolean existsParty(String cpaId, Party party)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> party.matches(p.getPartyId()))
						.flatMap(p -> p.getCollaborationRole().stream())
						.anyMatch(r -> party.getRole() == null || party.getRole().equals(r.getRole().getName())))
				.orElse(false);
	}

	public Optional<EbMSPartyInfo> getEbMSPartyInfo(String cpaId, Party party)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> party.matches(p.getPartyId()))
						.filter(p -> p.getCollaborationRole().stream().anyMatch(r -> party.getRole() == null || party.getRole().equals(r.getRole().getName())))
						.map(p -> createEbMSPartyInfo(party,p))
						.findFirst()
						.orElse(null));
	}

	private EbMSPartyInfo createEbMSPartyInfo(Party party, PartyInfo partyInfo)
	{
		EbMSPartyInfo result = new EbMSPartyInfo();
		result.setPartyIds(CPAUtils.toPartyId(party.getPartyId(partyInfo.getPartyId())));
		result.setRole(party.getRole());
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
	
	public Optional<Party> getFromParty(String cpaId, Role fromRole, String service, String action)
	{
		String partyId = fromRole.getPartyId() == null ? CPAUtils.toString(getFromPartyInfo(cpaId,fromRole,service,action)
				.orElseThrow(() -> StreamUtils.illegalStateException("FromPartyInfo",cpaId,fromRole,service,action)).getPartyIds().get(0)) :
					fromRole.getPartyId();
		return Optional.of(partyId)
			.map(id -> new Party(id,fromRole.getRole()));
	}
	
	public Optional<Party> getToParty(String cpaId, Role toRole, String service, String action)
	{
		String partyId = toRole.getPartyId() == null ? CPAUtils.toString(getToPartyInfo(cpaId,toRole,service,action)
				.orElseThrow(() -> StreamUtils.illegalStateException("ToPartyInfo",cpaId,toRole,service,action)).getPartyIds().get(0)) :
					toRole.getPartyId();
		return Optional.of(partyId)
				.map(id -> new Party(id,toRole.getRole()));
	}
	
	public Optional<FromPartyInfo> getFromPartyInfo(String cpaId, Role fromRole, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> fromRole == null || fromRole.matches(p.getPartyId()))
						.flatMap(p -> p.getCollaborationRole().stream()
								.filter(r -> fromRole == null || fromRole.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
								.flatMap(r -> r.getServiceBinding().getCanSend().stream()
										.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
										.map(cs -> CPAUtils.getFromPartyInfo(fromRole == null ? p.getPartyId().get(0) : fromRole.getPartyId(p.getPartyId()),r,cs))))
						.findFirst()
						.orElse(null));
	}

	public Optional<ToPartyInfo> getToPartyInfoByFromPartyActionBinding(String cpaId, Role fromRole, String service, String action)
	{
		return getFromPartyInfo(cpaId,fromRole,service,action)
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

	public Optional<ToPartyInfo> getToPartyInfo(String cpaId, Role toRole, String service, String action)
	{
		return getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> toRole == null || toRole.matches(p.getPartyId()))
						.flatMap(p -> p.getCollaborationRole().stream()
								.filter(r -> toRole == null || toRole.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
								.flatMap(r -> r.getServiceBinding().getCanReceive().stream()
									.filter(cr -> action.equals(cr.getThisPartyActionBinding().getAction()))
									.map(cr -> CPAUtils.getToPartyInfo(toRole == null ? p.getPartyId().get(0) : toRole.getPartyId(p.getPartyId()),r,cr))))
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
		if (Constants.EBMS_SERVICE_URI.equals(service))
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
		if (Constants.EBMS_SERVICE_URI.equals(service))
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
		Optional<CanSend> canSend =  getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
						.flatMap(p -> p.getCollaborationRole().stream())
						.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
						.flatMap(r -> r.getServiceBinding().getCanSend().stream())
						.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
						.findFirst()
						.orElse(null));
		DocExchange docExchange = CPAUtils.getDocExchange(
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
		Optional<CanSend> canSend =  getCPA(cpaId)
				.map(c -> c.getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanSend().stream())
				.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
				.findFirst()
				.orElse(null));
		DocExchange docExchange = CPAUtils.getDocExchange(
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
		return urlManager.getURL(CPAUtils.getUri(
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
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","existsCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCPAIds"));
	}

	public void setMethodCache(Ehcache methodCache)
	{
		this.methodCache = methodCache;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	public void setUrlManager(URLManager urlManager)
	{
		this.urlManager = urlManager;
	}
}
