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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.WithMessageFilter;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class MessageEventDAOImpl implements MessageEventDAO, WithMessageFilter
{
	public static class EbMSMessageEventRowMapper implements RowMapper<MessageEvent>
	{
		@Override
		public MessageEvent mapRow(ResultSet rs, int nr) throws SQLException
		{
			return new MessageEvent(rs.getString("message_id"),MessageEventType.values()[rs.getInt("event_type")]);
		}
	}

	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	public List<MessageEvent> getEbMSMessageEvents(MessageFilter messageFilter, MessageEventType[] types)
	{
		val parameters = new ArrayList<Object>();
		return jdbcTemplate.query(
			"select message_event.message_id, message_event.event_type" +
			" from message_event, ebms_message" +
			" where message_event.processed = 0" +
			" and message_event.event_type in (" + join(types == null || types.length == 0 ? MessageEventType.values() : types,",") + ")" +
			" and message_event.message_id = ebms_message.message_id" +
			" and ebms_message.message_nr = 0" +
			getMessageFilter(messageFilter,parameters) +
			" order by ebms_message.time_stamp asc",
			new EbMSMessageEventRowMapper(),
			parameters.toArray(new Object[0])
		);
	}

	private String getMessageEventsQuery(String messageContextFilter, MessageEventType[] types, int maxNr)
	{
		return "select message_event.message_id, message_event.event_type" +
				" from message_event, ebms_message" +
				" where message_event.processed = 0" +
				" and message_event.event_type in (" + join(types == null || types.length == 0 ? MessageEventType.values() : types,",") + ")" +
				" and message_event.message_id = ebms_message.message_id" +
				" and ebms_message.message_nr = 0" +
				messageContextFilter +
				" order by message_event.time_stamp asc" +
				" offset 0 rows" +
				" fetch first (" + maxNr + ") rows only";
	}

	@Override
	public List<MessageEvent> getEbMSMessageEvents(MessageFilter messageFilter, MessageEventType[] types, int maxNr)
	{
		val parameters = new ArrayList<Object>();
		val messageContextFilter = getMessageFilter(messageFilter,parameters);
		return jdbcTemplate.query(
			getMessageEventsQuery(messageContextFilter,types,maxNr),
			new EbMSMessageEventRowMapper(),
			parameters.toArray(new Object[0])
		);
	}

	@Override
	public String insertEbMSMessageEvent(String messageId, MessageEventType type)
	{
		jdbcTemplate.update
		(
			"insert into message_event (" +
				"message_id," +
				"event_type," +
				"time_stamp" +
			") values (?,?,?)",
			messageId,
			type.getId(),
			Timestamp.from(Instant.now())
		);
		return messageId;
	}

	@Override
	public int processEbMSMessageEvent(String messageId)
	{
		return jdbcTemplate.update
		(
			"update message_event" +
			" set processed = 1" +
			" where message_id = ?",
			messageId
		);
	}

	protected String join(MessageEventType[] array, String delimiter)
	{
		return Stream.of(array).mapToInt(MessageEventType::getId).mapToObj(String::valueOf).collect(Collectors.joining(delimiter));
	}
}
