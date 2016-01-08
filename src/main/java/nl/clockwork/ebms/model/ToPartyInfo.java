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

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CanReceive;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.ServiceType;

public class ToPartyInfo extends EbMSPartyInfo
{
	private static final long serialVersionUID = 1L;
	private String role;
	private ServiceType service;
	private CanReceive canReceive;
	
	public ToPartyInfo()
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

	public CanReceive getCanReceive()
	{
		return canReceive;
	}
	
	public void setCanReceive(CanReceive canReceive)
	{
		this.canReceive = canReceive;
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
