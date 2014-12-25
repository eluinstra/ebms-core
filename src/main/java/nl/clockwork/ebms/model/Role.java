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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;

public class Role extends Party
{
	private static final long serialVersionUID = 1L;

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
