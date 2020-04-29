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

import nl.clockwork.ebms.EbMSMessageEventType;
import nl.clockwork.ebms.dao.EbMSDAO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DAOEventListener implements EventListener
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private EbMSDAO ebMSDAO;

	public DAOEventListener()
	{
	}

	public DAOEventListener(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " received");
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.RECEIVED);
	}

	@Override
	public void onMessageDelivered(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " delivered");
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.DELIVERED);
	}
	
	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " failed");
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.FAILED);
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " expired");
		ebMSDAO.insertEbMSMessageEvent(messageId,EbMSMessageEventType.EXPIRED);
	}

	public void setEbMSDAO(EbMSDAO ebMSDAO)
	{
		this.ebMSDAO = ebMSDAO;
	}
}
