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
package nl.clockwork.ebms.event.processor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
abstract class EbMSEventDAOImpl implements EbMSEventDAO
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSEventRowMapper implements RowMapper<EbMSEvent>
	{
		public static final String SELECT = "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries";

		@Override
		public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return EbMSEvent.builder()
					.cpaId(rs.getString("cpa_id"))
					.sendDeliveryChannelId(rs.getString("send_channel_id"))
					.receiveDeliveryChannelId(rs.getString("receive_channel_id"))
					.messageId(rs.getString("message_id"))
					.timeToLive(rs.getTimestamp("time_to_live") != null ? rs.getTimestamp("time_to_live").toInstant() : null)
					.timestamp(rs.getTimestamp("time_stamp").toInstant())
					.confidential(rs.getBoolean("is_confidential"))
					.retries(rs.getInt("retries"))
					.build();
		}
	}

	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId)
	{
		return jdbcTemplate.query(
				EbMSEventRowMapper.SELECT +
				" from ebms_event" +
				" where time_stamp <= ?" +
				(serverId == null ? "" : " and server_id = '" + serverId + "'") +
				" order by time_stamp asc",
				new EbMSEventRowMapper(),
				Timestamp.from(timestamp));
	}

	public abstract String getEventsBeforeQuery(int maxNr, String serverId);

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId, int maxNr)
	{
		return jdbcTemplate.query(getEventsBeforeQuery(maxNr,serverId),new EbMSEventRowMapper(),Timestamp.from(timestamp));
	}
	
	@Override
	public String insertEvent(EbMSEvent event, String serverId)
	{
		jdbcTemplate.update(
				"insert into ebms_event (cpa_id,send_channel_id,receive_channel_id,message_id,time_to_live,time_stamp,is_confidential,retries,server_id) values (?,?,?,?,?,?,?,?,?)",
				event.getCpaId(),
				event.getSendDeliveryChannelId(),
				event.getReceiveDeliveryChannelId(),
				event.getMessageId(),
				event.getTimeToLive() != null ? Timestamp.from(event.getTimeToLive()) : null,
				Timestamp.from(event.getTimestamp()),
				event.isConfidential(),
				event.getRetries(),
				serverId);
		return event.getMessageId();
	}

	@Override
	public int updateEvent(EbMSEvent event)
	{
		return jdbcTemplate.update("update ebms_event set time_stamp = ?, retries = ? where message_id = ?",Timestamp.from(event.getTimestamp()),event.getRetries(),event.getMessageId());
	}
	
	@Override
	public int deleteEvent(String messageId)
	{
		return jdbcTemplate.update("delete from ebms_event where message_id = ?",messageId);
	}

	@Override
	public void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage)
	{
		jdbcTemplate.update(
				"insert into ebms_event_log (message_id,time_stamp,uri,status,error_message) values (?,?,?,?,?)",
				messageId,
				Timestamp.from(timestamp),
				uri,
				status.getId(),
				errorMessage);
	}
}
