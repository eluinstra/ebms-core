package nl.clockwork.ebms.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import nl.clockwork.ebms.util.CPAUtils;

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
	
	public boolean equals(List<PartyId> partyIds)
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
}
