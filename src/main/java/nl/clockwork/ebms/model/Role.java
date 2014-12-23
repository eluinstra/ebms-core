package nl.clockwork.ebms.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;

public class Role extends Party
{
	public Role()
	{
	}

	public Role(String role)
	{
		super(null,role);
	}

	public Role(String partyId, String role)
	{
		super(partyId,role);
	}

	@Override
	@XmlElement
	public String getPartyId()
	{
		return super.getPartyId();
	}

	@Override
	@XmlElement(required=true)
	public String getRole()
	{
		return super.getRole();
	}
	
	public boolean matches(List<PartyId> partyIds)
	{
		return getPartyId() == null || super.matches(partyIds);
	}

	public boolean matches(org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Role role)
	{
		return getRole().equals(role.getName());
	}
}
