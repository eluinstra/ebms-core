package nl.clockwork.ebms.model;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanSend;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;

public class FromPartyInfo extends MyPartyInfo
{
	private static final long serialVersionUID = 1L;
	private String role;
	private ServiceType service;
	private CanSend canSend;
	
	public FromPartyInfo()
	{
	}

	public ServiceType getService()
	{
		return service;
	}

	public void setService(ServiceType service)
	{
		this.service = service;
	}

	public CanSend getCanSend()
	{
		return canSend;
	}

	public void setCanSend(CanSend canSend)
	{
		this.canSend = canSend;
	}

	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}
	
	public DeliveryChannel getDeliveryChannel()
	{
		return Constants.EBMS_SERVICE_URI.equals(service.getValue()) ? defaultMshChannelId : CPAUtils.getDeliveryChannel(canSend.getThisPartyActionBinding().getChannelId());
	}
}
