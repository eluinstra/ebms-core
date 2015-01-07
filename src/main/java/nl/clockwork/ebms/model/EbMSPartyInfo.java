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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

public class EbMSPartyInfo implements Serializable
{
	private static final long serialVersionUID = 1L;
	DeliveryChannel defaultMshChannelId;
	private List<PartyId> partyIds;
	private String role;

	public EbMSPartyInfo()
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

	public List<PartyId> getPartyIds()
	{
		return partyIds;
	}

	public void setPartyIds(List<PartyId> partyIds)
	{
		this.partyIds = partyIds;
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
