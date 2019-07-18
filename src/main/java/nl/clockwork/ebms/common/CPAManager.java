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

import org.ehcache.core.Ehcache;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PersistenceLevelType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.SyncReplyModeType;

import nl.clockwork.ebms.Constants;
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
	private Ehcache<String,Object> methodCache;
	private EbMSDAO ebMSDAO;
	private URLManager urlManager;

	public boolean existsCPA(String cpaId)
	{
		return ebMSDAO.existsCPA(cpaId);
	}

	public CollaborationProtocolAgreement getCPA(String cpaId)
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
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		return StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart()) >= 0
				&& timestamp.compareTo(cpa.getEnd()) <= 0;
	}

	public boolean existsParty(String cpaId, Party party)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> party.matches(p.getPartyId()))
				.flatMap(p -> p.getCollaborationRole().stream())
				.anyMatch(r -> party.getRole() == null || party.getRole().equals(r.getRole().getName()));
	}

	public EbMSPartyInfo getEbMSPartyInfo(String cpaId, Party party)
	{
		return getCPA(cpaId).getPartyInfo().stream()
			.filter(p -> party.matches(p.getPartyId()))
			.filter(p -> p.getCollaborationRole().stream().anyMatch(r -> party.getRole() == null || party.getRole().equals(r.getRole().getName())))
			.map(p -> 
			{
				EbMSPartyInfo result = new EbMSPartyInfo();
				result.setPartyIds(CPAUtils.toPartyId(party.getPartyId(p.getPartyId())));
				result.setRole(party.getRole());
				return result;
			})
			.findFirst()
			.orElse(null);
	}

	public PartyInfo getPartyInfo(String cpaId, CacheablePartyId partyId)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.findFirst()
				.orElse(null);
	}
	
	public Party getFromParty(String cpaId, Role fromRole, String service, String action)
	{
		String partyId = fromRole.getPartyId() == null ? CPAUtils.toString(getFromPartyInfo(cpaId,fromRole,service,action).getPartyIds().get(0)) : fromRole.getPartyId();
		return new Party(partyId,fromRole.getRole());
	}
	
	public Party getToParty(String cpaId, Role toRole, String service, String action)
	{
		String partyId = toRole.getPartyId() == null ? CPAUtils.toString(getToPartyInfo(cpaId,toRole,service,action).getPartyIds().get(0)) : toRole.getPartyId();
		return new Party(partyId,toRole.getRole());
	}
	
	public FromPartyInfo getFromPartyInfo(String cpaId, Role fromRole, String service, String action)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> fromRole == null || fromRole.matches(p.getPartyId()))
				.flatMap(p -> p.getCollaborationRole().stream()
						.filter(r -> fromRole == null || fromRole.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
						.flatMap(r -> r.getServiceBinding().getCanSend().stream()
								.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
								.map(cs -> CPAUtils.getFromPartyInfo(
										fromRole == null ? p.getPartyId().get(0) : fromRole.getPartyId(p.getPartyId()),
										r,
										cs))))
				.findFirst()
				.orElse(null);
	}

	public ToPartyInfo getToPartyInfoByFromPartyActionBinding(String cpaId, Role fromRole, String service, String action)
	{
		FromPartyInfo fromPartyInfo = getFromPartyInfo(cpaId,fromRole,service,action);
		return getCPA(cpaId).getPartyInfo().stream()
				.flatMap(p -> p.getCollaborationRole().stream()
						.flatMap(r -> r.getServiceBinding().getCanReceive().stream()
								.filter(cr -> cr.getThisPartyActionBinding().equals(fromPartyInfo.getCanSend().getOtherPartyActionBinding()))
								.map(cr -> CPAUtils.getToPartyInfo(p.getPartyId().get(0),r,cr))))
				.findFirst()
				.orElse(null);
	}

	public ToPartyInfo getToPartyInfo(String cpaId, Role toRole, String service, String action)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> toRole == null || toRole.matches(p.getPartyId()))
				.flatMap(p -> p.getCollaborationRole().stream()
						.filter(r -> toRole == null || toRole.matches(r.getRole()) && service.equals(CPAUtils.toString(r.getServiceBinding().getService())))
						.flatMap(r -> r.getServiceBinding().getCanReceive().stream()
							.filter(cr -> action.equals(cr.getThisPartyActionBinding().getAction()))
							.map(cr -> CPAUtils.getToPartyInfo(
									toRole == null ? p.getPartyId().get(0) : toRole.getPartyId(p.getPartyId()),
									r,
									cr))))
				.findFirst()
				.orElse(null);
	}

	public boolean canSend(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanSend().stream())
				.anyMatch(cs -> action.equals(cs.getThisPartyActionBinding().getAction()));
	}

	public boolean canReceive(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanReceive().stream())
				.anyMatch(cr -> action.equals(cr.getThisPartyActionBinding().getAction()));
	}

	public DeliveryChannel getDeliveryChannel(String cpaId, String deliveryChannelId)
	{
		return getCPA(cpaId).getPartyInfo().stream()
				.flatMap(p -> p.getDeliveryChannel().stream())
				.filter(d -> d.getChannelId().equals(deliveryChannelId))
				.findFirst()
				.orElse(null);
	}

	public DeliveryChannel getDefaultDeliveryChannel(String cpaId, CacheablePartyId partyId, String action)
	{
//		return getCPA(cpaId).getPartyInfo().stream()
//				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
//				.flatMap(p -> p.getOverrideMshActionBinding().stream())
//				.filter(b -> b.getAction().equals(action))
//				.findFirst()
//				.map(b -> (DeliveryChannel)b.getChannelId())
//				.orElse(getCPA(cpaId).getPartyInfo().stream()
//						.filter(p -> CPAUtils.equals(p.getPartyId(),partyId)).map(p -> (DeliveryChannel)p.getDefaultMshChannelId()).findFirst().orElse(null));
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		if (partyInfo == null) return null;
		return partyInfo.getOverrideMshActionBinding().stream()
				.filter(b -> b.getAction().equals(action))
				.findFirst()
				.map(b -> (DeliveryChannel)b.getChannelId())
				.orElse((DeliveryChannel)partyInfo.getDefaultMshChannelId());
	}

	public DeliveryChannel getSendDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		if (Constants.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
			return 
					partyInfo.getCollaborationRole().stream()
					.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
					.flatMap(r -> r.getServiceBinding().getCanReceive().stream())
					.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
					.findFirst()
					.map(cs -> CPAUtils.getDeliveryChannel(cs.getThisPartyActionBinding().getChannelId()))
					.orElse(null);
	}
	
	public DeliveryChannel getReceiveDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		if (Constants.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
			return 
					partyInfo.getCollaborationRole().stream()
					.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
					.flatMap(r -> r.getServiceBinding().getCanReceive().stream())
					.filter(cr -> action.equals(cr.getThisPartyActionBinding().getAction()))
					.findFirst()
					.map(cr -> CPAUtils.getDeliveryChannel(cr.getThisPartyActionBinding().getChannelId()))
					.orElse(null);
	}
	
	public boolean isNonRepudiationRequired(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		CanSend canSend =  getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanSend().stream())
				.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
				.findFirst()
				.orElse(null);
		DocExchange docExchange = CPAUtils.getDocExchange(getSendDeliveryChannel(cpaId,partyId,role,service,action));
		return canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired() && docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null;
	}

	public boolean isConfidential(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		CanSend canSend =  getCPA(cpaId).getPartyInfo().stream()
				.filter(p -> CPAUtils.equals(p.getPartyId(),partyId))
				.flatMap(p -> p.getCollaborationRole().stream())
				.filter(r -> role.equals(r.getRole().getName()) && CPAUtils.toString(r.getServiceBinding().getService()).equals(service))
				.flatMap(r -> r.getServiceBinding().getCanSend().stream())
				.filter(cs -> action.equals(cs.getThisPartyActionBinding().getAction()))
				.findFirst()
				.orElse(null);
		DocExchange docExchange = CPAUtils.getDocExchange(getSendDeliveryChannel(cpaId,partyId,role,service,action));
		return (PersistenceLevelType.PERSISTENT.equals(canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential()) || PersistenceLevelType.TRANSIENT_AND_PERSISTENT.equals(canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().getIsConfidential())) && docExchange.getEbXMLReceiverBinding() != null && docExchange.getEbXMLReceiverBinding().getReceiverDigitalEnvelope() != null;
	}

	public String getUri(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return urlManager.getURL(CPAUtils.getUri(getReceiveDeliveryChannel(cpaId,partyId,role,service,action)));
	}

	public SyncReplyModeType getSyncReply(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		DeliveryChannel deliveryChannel = getSendDeliveryChannel(cpaId,partyId,role,service,action);
		return deliveryChannel.getMessagingCharacteristics().getSyncReplyMode();
	}

	private void flushCPAMethodCache(String cpaId)
	{
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","existsCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("EbMSDAOImpl","getCPAIds"));
	}

	public void setMethodCache(Ehcache<String,Object> methodCache)
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
