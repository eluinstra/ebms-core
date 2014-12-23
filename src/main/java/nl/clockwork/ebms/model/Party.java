package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import nl.clockwork.ebms.util.CPAUtils;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Role;

public class Party implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String partyId;
	private String role;
	
	public Party()
	{
	}

	public Party(String partyId)
	{
		this.partyId = partyId;
	}

	public Party(String partyId, String role)
	{
		this.partyId = partyId;
		this.role = role;
	}

	@XmlElement(required=true)
	public String getPartyId()
	{
		return partyId;
	}

	public void setPartyId(String partyId)
	{
		this.partyId = partyId;
	}

	@XmlElement
	public String getRole()
	{
		return role;
	}

	public void setRole(String role)
	{
		this.role = role;
	}
	
	public boolean matches(List<PartyId> partyIds)
	{
		if (getPartyId() == null && (partyIds == null || partyIds.size() == 0))
			return true;
		if (getPartyId() == null || partyIds == null)
			return false;
		for (PartyId partyId : partyIds)
			if (getPartyId().equals(CPAUtils.toString(partyId)))
				return true;
		return false;
	}
	
	public boolean matches(Role role)
	{
		return getRole() == null || getRole().equals(role.getName());
	}
}
