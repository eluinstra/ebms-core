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
package nl.clockwork.ebms.event.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoggingEventListener implements EventListener
{
	protected transient Log logger = LogFactory.getLog(getClass());

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " received");
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " delivered");
	}
	
	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " failed");
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " expired");
	}
}
