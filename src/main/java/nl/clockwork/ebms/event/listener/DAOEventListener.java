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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class DAOEventListener extends LoggingEventListener
{
	@NonNull
	EbMSDAO ebMSDAO;

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.RECEIVED);
		super.onMessageReceived(messageId);
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.DELIVERED);
		super.onMessageDelivered(messageId);
	}
	
	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.FAILED);
		super.onMessageFailed(messageId);
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.EXPIRED);
		super.onMessageExpired(messageId);
	}
}
