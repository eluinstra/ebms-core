/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.mule.ebms.model;

import nl.clockwork.mule.ebms.model.ebxml.From;
import nl.clockwork.mule.ebms.model.ebxml.Service;
import nl.clockwork.mule.ebms.model.ebxml.To;

public class EbMSChannel
{
	private String cpaId;
	private String id;
	private From from;
	private To to;
	private Service service;
	private String action;
	private String endpoint;

	public EbMSChannel(String id, String cpaId, From from, To to, Service service, String action, String endpoint)
	{
		this.cpaId = cpaId;
		this.id = id;
		this.from = from;
		this.to = to;
		this.service = service;
		this.action = action;
		this.endpoint = endpoint;
	}

	public String getCpaId()
	{
		return cpaId;
	}

	public String getId()
	{
		return id;
	}

	public From getFrom()
	{
		return from;
	}

	public To getTo()
	{
		return to;
	}

	public Service getService()
	{
		return service;
	}

	public String getAction()
	{
		return action;
	}

	public String getEndpoint()
	{
		return endpoint;
	}
	
	
}
