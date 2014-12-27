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
package nl.clockwork.ebms.service;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class GetCPAIdsResponse
{
	private List<String> cpaIds;

	public GetCPAIdsResponse()
	{
	}

	@XmlElementWrapper(nillable=true,name="CPAIds")
	@XmlElement(name="CPAId")
	public List<String> getCpaIds()
	{
		return cpaIds;
	}
	
	public void setCpaIds(List<String> cpaIds)
	{
		this.cpaIds = cpaIds;
	}
}
