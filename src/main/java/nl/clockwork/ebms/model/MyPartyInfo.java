package nl.clockwork.ebms.model;

import java.io.Serializable;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;

public class MyPartyInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	DeliveryChannel defaultMshChannelId;
	private PartyId partyId;
	private String role;
	
	public MyPartyInfo()
	{
	}

	public DeliveryChannel getDefaultMshChannelId()
	{
		return defaultMshChannelId;
	}
	
	public void setDefaultMshChannelId(DeliveryChannel defaultMshChannelId)
	{
		this.defaultMshChannelId = defaultMshChannelId;
	}
	
	public PartyId getPartyId()
	{
		return partyId;
	}

	public void setPartyId(PartyId partyId)
	{
		this.partyId = partyId;
	}

	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}
}
