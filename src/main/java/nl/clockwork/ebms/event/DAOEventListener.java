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
package nl.clockwork.ebms.event;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DAOEventListener implements EventListener
{
	public static enum EventType
	{
		RECEIVED,ACKNOWLEDGED,FAILED,EXPIRED;
	}
	protected transient Log logger = LogFactory.getLog(getClass());
	private JdbcTemplate jdbcTemplate;

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " received");
			insertEbMSMessageEvent(messageId,EventType.RECEIVED);
		}
		catch (DataAccessException e)
		{
			throw new EventException(e);
		}
	}

	@Override
	public void onMessageAcknowledged(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " acknowledged");
			insertEbMSMessageEvent(messageId,EventType.ACKNOWLEDGED);
		}
		catch (DataAccessException e)
		{
			throw new EventException(e);
		}
	}
	
	@Override
	public void onMessageFailed(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " failed");
			insertEbMSMessageEvent(messageId,EventType.FAILED);
		}
		catch (DataAccessException e)
		{
			throw new EventException(e);
		}
	}

	@Override
	public void onMessageExpired(String messageId) throws EventException
	{
		try
		{
			logger.info("Message " + messageId + " expired");
			insertEbMSMessageEvent(messageId,EventType.EXPIRED);
		}
		catch (DataAccessException e)
		{
			throw new EventException(e);
		}
	}

	private void insertEbMSMessageEvent(String messageId, EventType eventType)
	{
		jdbcTemplate.update
		(
			"insert into ebms_message_event (" +
				"message_id," +
				"event_type," +
				"time_stamp" +
			") values (?,?,?)",
			messageId,
			eventType.ordinal(),
			new Date()
		);
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
	{
		this.jdbcTemplate = jdbcTemplate;
	}
}
