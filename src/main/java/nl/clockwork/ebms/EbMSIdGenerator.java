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
package nl.clockwork.ebms;

import java.util.UUID;

public class EbMSIdGenerator
{
	public String generateConversationId()
	{
		return UUID.randomUUID().toString();
	}

	public String generateMessageId(String hostname)
	{
		return UUID.randomUUID().toString() + "@" + hostname;
	}

	public String createMessageId(String hostname, String conversationId)
	{
		return conversationId + "@" + hostname;
	}

	public String createMessageId(String hostname, String conversationId, String messageId)
	{
		return (messageId == null ? conversationId : messageId) + "@" + hostname;
	}

}
