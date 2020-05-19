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
package nl.clockwork.ebms.service.model;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.PartyId;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
//@RequiredArgsConstructor
public class Party implements Serializable
{
	private static final long serialVersionUID = 1L;
	@XmlElement(required=true)
	//@NonNull
	String partyId;
	@XmlElement(required=true)
	//@NonNull
	String role;

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
	
	public boolean matches(org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.Role role)
	{
		return getRole().equals(role.getName());
	}
}
