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

public class Channel
{
	private int id;
	private String channelId;
	private String cpaId;
	private String actionId;
	private String endpoint;

	public Channel(int id, String channelId, String cpaId, String actionId, String endpoint)
	{
		this.id = id;
		this.channelId = channelId;
		this.cpaId = cpaId;
		this.actionId = actionId;
		this.endpoint = endpoint;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	public String getChannelId()
	{
		return channelId;
	}
	public void setChannelId(String channelId)
	{
		this.channelId = channelId;
	}
	public String getCpaId()
	{
		return cpaId;
	}
	public void setCpaId(String cpaId)
	{
		this.cpaId = cpaId;
	}
	public String getActionId()
	{
		return actionId;
	}
	public void setActionId(String actionId)
	{
		this.actionId = actionId;
	}
	public String getEndpoint()
	{
		return endpoint;
	}
	public void setEndpoint(String endpoint)
	{
		this.endpoint = endpoint;
	}

}
