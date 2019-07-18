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
package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;

import nl.clockwork.ebms.util.CPAUtils;

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

	@Override
	public String toString()
	{
		return new StringBuffer().append(partyId).append(":").append(role).toString();
	}
	
	public PartyId getPartyId(List<PartyId> partyIds)
	{
		if (getPartyId() == null || partyIds == null)
			return null;
		return partyIds.stream().filter(id -> getPartyId().equals(CPAUtils.toString(id))).findFirst().orElse(null);
	}

	public boolean matches(List<PartyId> partyIds)
	{
		if (getPartyId() == null && (partyIds == null || partyIds.size() == 0))
			return true;
		if (getPartyId() == null || partyIds == null)
			return false;
		return partyIds.stream().anyMatch(id -> getPartyId().equals(CPAUtils.toString(id)));
	}

}
