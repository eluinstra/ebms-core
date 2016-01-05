package nl.clockwork.ebms.common;

import java.util.Date;
import java.util.List;

import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSPartyInfo;
import nl.clockwork.ebms.model.FromPartyInfo;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.model.ToPartyInfo;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ActionBindingType;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationRole;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyInfo;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.StatusValueType;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;

public class CPAManager
{
	private EbMSDAO ebMSDAO;
	private URLManager urlManager;

	public boolean existsCPA(String cpaId)
	{
		return getCPA(cpaId) != null;
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
	}

	public int updateCPA(CollaborationProtocolAgreement cpa)
	{
		return ebMSDAO.updateCPA(cpa);
	}

	public int deleteCPA(String cpaId)
	{
		return ebMSDAO.deleteCPA(cpaId);
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
	
	public EbMSPartyInfo getEbMSPartyInfo(String cpaId, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (CPAUtils.equals(partyInfo.getPartyId(),partyIds))
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

	public PartyInfo getPartyInfo(String cpaId, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			if (CPAUtils.equals(partyInfo.getPartyId(),partyIds))
				return partyInfo;
		return null;
	}
	
	public FromPartyInfo getFromPartyInfo(String cpaId, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds, String fromRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyIds);
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

	public ToPartyInfo getToPartyInfo(String cpaId, ActionBindingType actionBinding)
	{
		CollaborationProtocolAgreement cpa = getCPA(cpaId);
		for (PartyInfo partyInfo : cpa.getPartyInfo())
			for (CollaborationRole role : partyInfo.getCollaborationRole())
				for (CanReceive canReceive : role.getServiceBinding().getCanReceive())
					if (canReceive.getThisPartyActionBinding().equals(actionBinding))
						return CPAUtils.getToPartyInfo(partyInfo,role,canReceive);
		return null;
	}

	public ToPartyInfo getToPartyInfo(String cpaId, List<org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId> partyIds, String toRole, String service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyIds);
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

	public String getOriginalUri(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		PartyInfo partyInfo = getPartyInfo(cpaId,partyId);
		DeliveryChannel deliveryChannel = CPAUtils.getToDeliveryChannel(partyInfo,role,service,action);
		return urlManager.getUrl(CPAUtils.getUri(deliveryChannel));
	}

	public String getUri(String cpaId, List<PartyId> partyId, String role, Service service, String action)
	{
		return ebMSDAO.getUrl(getOriginalUri(cpaId,partyId,role,service,action));
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
