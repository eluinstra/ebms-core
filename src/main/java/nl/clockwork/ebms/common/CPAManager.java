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

import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;

import net.sf.ehcache.Ehcache;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DocExchange;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.OverrideMshActionBinding;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceBinding;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;

public class CPAManager
{
	private Ehcache methodCache;
	private EbMSDAO ebMSDAO;

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

	public void insertCPA(CollaborationProtocolAgreement cpa, String url)
	{
		ebMSDAO.insertCPA(cpa,url);
		flushUrlMethodCache(cpa.getCpaid());
		flushCPAMethodCache(cpa.getCpaid());
	}

	public int updateCPA(CollaborationProtocolAgreement cpa, String url)
	{
		int result = ebMSDAO.updateCPA(cpa,url);
		flushAllMethodCache();
		return result;
	}

	public int deleteCPA(String cpaId)
	{
		int result = ebMSDAO.deleteCPA(cpaId);
		flushAllMethodCache();
		return result;
	}

	public String getUrl(String cpaId)
	{
		return ebMSDAO.getUrl(cpaId);
	}

	public void setUrl(String cpaId, String url)
	{
		ebMSDAO.updateUrl(cpaId,url);
		flushUrlMethodCache(cpaId);
	}

	private void flushCPAMethodCache(String cpaId)
	{
		methodCache.remove(MethodCacheInterceptor.getCacheKey(Proxy.getInvocationHandler(ebMSDAO).getClass().getName(),"getCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("nl.clockwork.ebms.common.CPAManager","existsCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("nl.clockwork.ebms.common.CPAManager","getCPA",cpaId));
		methodCache.remove(MethodCacheInterceptor.getCacheKey("nl.clockwork.ebms.common.CPAManager","getCPAIds"));
	}

	private void flushUrlMethodCache(String cpaId)
	{
		methodCache.remove(MethodCacheInterceptor.getCacheKey(Proxy.getInvocationHandler(ebMSDAO).getClass().getName(),"getUrl",cpaId));
	}

	private void flushAllMethodCache()
	{
		methodCache.removeAll();
	}

	public boolean isValid(String cpaId, Date timestamp)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		return StatusValueType.AGREED.equals(cpa.getStatus().getValue())
				&& timestamp.compareTo(cpa.getStart()) >= 0
				&& timestamp.compareTo(cpa.getEnd()) <= 0;
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
	
	public EbMSPartyInfo getEbMSPartyInfo(String cpaId, CacheablePartyId partyId)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (CPAUtils.equals(partyInfo.getPartyId(),partyId))
			{
				EbMSPartyInfo result = new EbMSPartyInfo();
				result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
				result.setPartyIds(CPAUtils.getPartyIds(partyInfo.getPartyId()));
				return result;
			}
		return null;
	}
	
	public EbMSPartyInfo getEbMSPartyInfo(String cpaId, Party party)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (party.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (party.matches(role.getRole()))
					{
						EbMSPartyInfo result = new EbMSPartyInfo();
						result.setDefaultMshChannelId((DeliveryChannel)partyInfo.getDefaultMshChannelId());
						result.setPartyIds(CPAUtils.getPartyIds(partyInfo.getPartyId()));
						result.setRole(party.getRole());
						return result;
					}
		return null;
	}

	public PartyInfo getPartyInfo(String cpaId, CacheablePartyId partyId)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (CPAUtils.equals(partyInfo.getPartyId(),partyId))
				return partyInfo;
		return null;
	}
	
	public FromPartyInfo getFromPartyInfo(String cpaId, CacheablePartyId partyId, String fromRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		for (CollaborationRole role : partyInfo.getCollaborationRole())
			if (fromRole.equals(role.getRole().getName()) && service.equals(CPAUtils.toString(role.getServiceBinding().getService())))
				for (CanSend canSend : role.getServiceBinding().getCanSend())
					if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						return CPAUtils.getFromPartyInfo(partyInfo,role,canSend);
		return null;
	}
	
	public FromPartyInfo getFromPartyInfo(String cpaId, Role fromRole, String service, String action)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (fromRole == null || fromRole.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (fromRole == null || fromRole.matches(role.getRole()) && service.equals(CPAUtils.toString(role.getServiceBinding().getService())))
						for (CanSend canSend : role.getServiceBinding().getCanSend())
							if (action.equals(canSend.getThisPartyActionBinding().getAction()))
								return CPAUtils.getFromPartyInfo(partyInfo,role,canSend);
		return null;
	}

	public ToPartyInfo getToPartyInfoByFromPartyActionBinding(String cpaId, Role fromRole, String service, String action)
	{
		FromPartyInfo fromPartyInfo = getFromPartyInfo(cpaId,fromRole,service,action);
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (CollaborationRole role : partyInfo.getCollaborationRole())
				for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
					if (canReceive.getThisPartyActionBinding().equals(fromPartyInfo.getCanSend().getOtherPartyActionBinding()))
						return CPAUtils.getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}

	public ToPartyInfo getToPartyInfo(String cpaId, CacheablePartyId partyId, String toRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		for (CollaborationRole role : partyInfo.getCollaborationRole())
			if (toRole.equals(role.getRole().getName()) && service.equals(CPAUtils.toString(role.getServiceBinding().getService())))
				for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
					if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						return CPAUtils.getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}
	
	public ToPartyInfo getToPartyInfo(String cpaId, Role toRole, String service, String action)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (toRole == null || toRole.matches(partyInfo.getPartyId()))
				for (CollaborationRole role : partyInfo.getCollaborationRole())
					if (toRole == null || toRole.matches(role.getRole()) && service.equals(CPAUtils.toString(role.getServiceBinding().getService())))
						for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
							if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
								return CPAUtils.getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}

	private ServiceBinding getServiceBinding(PartyInfo partyInfo, String role, String service)
	{
		for (CollaborationRole collaborationRole : partyInfo.getCollaborationRole())
			if (role.equals(collaborationRole.getRole().getName()) && CPAUtils.toString(collaborationRole.getServiceBinding().getService()).equals(service))
				return collaborationRole.getServiceBinding();
		return null;
	}

	private CanSend getCanSend(PartyInfo partyInfo, String role, String service, String action)
	{
		ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
		if (serviceBinding != null)
			for (CanSend canSend : serviceBinding.getCanSend())
				if (action.equals(canSend.getThisPartyActionBinding().getAction()))
					return canSend;
		return null;
	}

	private CanReceive getCanReceive(PartyInfo partyInfo, String role, String service, String action)
	{
		ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
		if (serviceBinding != null)
			for (CanReceive canReceive : serviceBinding.getCanReceive())
				if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
					return canReceive;
		return null;
	}

	public boolean canSend(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return getCanSend(getPartyInfo(cpaId,partyId),role,service,action) != null;
	}

	public boolean canReceive(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		return getCanReceive(getPartyInfo(cpaId,partyId),role,service,action) != null;
	}

	public DeliveryChannel getDefaultDeliveryChannel(String cpaId, CacheablePartyId partyId, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		for (OverrideMshActionBinding overrideMshActionBinding : partyInfo.getOverrideMshActionBinding())
			if (overrideMshActionBinding.getAction().equals(action))
				return (DeliveryChannel)overrideMshActionBinding.getChannelId();
		return (DeliveryChannel)partyInfo.getDefaultMshChannelId();
	}

	public DeliveryChannel getFromDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		if (Constants.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo, role, service);
			if (serviceBinding != null)
				for (CanSend canSend : serviceBinding.getCanSend())
					if (action.equals(canSend.getThisPartyActionBinding().getAction()))
						return CPAUtils.getDeliveryChannel(canSend.getThisPartyActionBinding().getChannelId());
		}
		return null;
	}
	
	public DeliveryChannel getToDeliveryChannel(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		if (Constants.EBMS_SERVICE_URI.equals(service))
			return getDefaultDeliveryChannel(cpaId,partyId,action);
		else
		{
			ServiceBinding serviceBinding = getServiceBinding(partyInfo,role,service);
			if (serviceBinding != null)
				for (CanReceive canReceive : serviceBinding.getCanReceive())
					if (action.equals(canReceive.getThisPartyActionBinding().getAction()))
						return CPAUtils.getDeliveryChannel(canReceive.getThisPartyActionBinding().getChannelId());
		}
		return null;
	}
	
	public boolean isNonRepudiationRequired(String cpaId, CacheablePartyId partyId, String role, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		CanSend canSend = getCanSend(partyInfo,role,service,action);
		DocExchange docExchange = CPAUtils.getDocExchange(getFromDeliveryChannel(cpaId,partyId,role,service,action));
		return canSend.getThisPartyActionBinding().getBusinessTransactionCharacteristics().isIsNonRepudiationRequired() && docExchange.getEbXMLSenderBinding() != null && docExchange.getEbXMLSenderBinding().getSenderNonRepudiation() != null;
	}

	public String getUri(String cpaId, DeliveryChannel deliveryChannel)
	{
		String replacementUrl = getUrl(cpaId);
		return replacementUrl == null ? CPAUtils.getUri(deliveryChannel) : replacementUrl;
	}

	public void setMethodCache(Ehcache methodCache)
	{
		this.methodCache = methodCache;
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

}
