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
package nl.clockwork.ebms.common;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class EbMSIdGenerator
{
	private final String serverId;
	
	public EbMSIdGenerator(String serverId)
	{
		this.serverId = StringUtils.isBlank(serverId) ? "" : "_" + serverId;
	}

	public String generateMessageId(String hostname)
	{
		return UUID.randomUUID().toString() + serverId + "@" + hostname;
	}

	public String generateMessageId(String hostname, String conversationId)
	{
		return conversationId + "@" + hostname;
	}

	public String generateConversationId()
	{
		return UUID.randomUUID().toString() + serverId;
	}

}
