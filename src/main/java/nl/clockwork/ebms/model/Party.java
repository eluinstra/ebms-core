package nl.clockwork.ebms.model;

import javax.xml.bind.annotation.XmlElement;

public class Party
{
	String partyId;
	String role;
	
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
	
}
