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
package nl.clockwork.ebms.delivery.task;

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
abstract class DeliveryTaskDAOImpl implements DeliveryTaskDAO
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSEventRowMapper implements RowMapper<DeliveryTask>
	{
		public static final String SELECT = "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries";

		@Override
		public DeliveryTask mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return DeliveryTask.builder()
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
	public List<DeliveryTask> getTasksBefore(Instant timestamp, String serverId)
	{
		return jdbcTemplate.query(
				EbMSEventRowMapper.SELECT +
				" from delivery_task" +
				" where time_stamp <= ?" +
				(serverId == null ? "" : " and server_id = '" + serverId + "'") +
				" order by time_stamp asc",
				new EbMSEventRowMapper(),
				Timestamp.from(timestamp));
	}

	public abstract String getTasksBeforeQuery(int maxNr, String serverId);

	@Override
	public List<DeliveryTask> getTasksBefore(Instant timestamp, String serverId, int maxNr)
	{
		return jdbcTemplate.query(getTasksBeforeQuery(maxNr,serverId),new EbMSEventRowMapper(),Timestamp.from(timestamp));
	}
	
	@Override
	public String insertTask(DeliveryTask task, String serverId)
	{
		jdbcTemplate.update(
				"insert into delivery_task (cpa_id,send_channel_id,receive_channel_id,message_id,time_to_live,time_stamp,is_confidential,retries,server_id) values (?,?,?,?,?,?,?,?,?)",
				task.getCpaId(),
				task.getSendDeliveryChannelId(),
				task.getReceiveDeliveryChannelId(),
				task.getMessageId(),
				task.getTimeToLive() != null ? Timestamp.from(task.getTimeToLive()) : null,
				Timestamp.from(task.getTimestamp()),
				task.isConfidential(),
				task.getRetries(),
				serverId);
		return task.getMessageId();
	}

	@Override
	public int updateTask(DeliveryTask task)
	{
		return jdbcTemplate.update("update delivery_task set time_stamp = ?, retries = ? where message_id = ?",Timestamp.from(task.getTimestamp()),task.getRetries(),task.getMessageId());
	}
	
	@Override
	public int deleteTask(String messageId)
	{
		return jdbcTemplate.update("delete from delivery_task where message_id = ?",messageId);
	}

	@Override
	public void insertLog(String messageId, Instant timestamp, String uri, DeliveryTaskStatus status, String errorMessage)
	{
		jdbcTemplate.update(
				"insert into delivery_log (message_id,time_stamp,uri,status,error_message) values (?,?,?,?,?)",
				messageId,
				Timestamp.from(timestamp),
				uri,
				status.getId(),
				errorMessage);
	}
}
