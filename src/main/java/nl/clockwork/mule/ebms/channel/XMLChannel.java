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
package nl.clockwork.mule.ebms.channel;


public class XMLChannel
{
	private String id;
	private String service;
	private String from;
	private String to;
	private String action;
	private String endpoint;

	public XMLChannel(String id, String service, String from, String to, String action, String endpoint)
	{
		this.id = id;
		this.service = service;
		this.from = from;
		this.to = to;
		this.action = action;
		this.endpoint = endpoint;
	}
	
	public String getId()
	{
		return id;
	}

	public String getService()
	{
		return service;
	}

	public String getFrom()
	{
		return from;
	}

	public String getTo()
	{
		return to;
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
