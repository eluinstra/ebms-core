/*
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
package nl.clockwork.ebms.event;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class LoggingMessageEventListener implements MessageEventListener
{
	@Override
	public void onMessageReceived(String messageId) throws MessageEventException
	{
		log.info("Received message " + messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws MessageEventException
	{
		log.info("Delivered message " + messageId);
	}

	@Override
	public void onMessageFailed(String messageId) throws MessageEventException
	{
		log.info("Failed message " + messageId);
	}

	@Override
	public void onMessageExpired(String messageId) throws MessageEventException
	{
		log.info("Expired message " + messageId);
	}
}
