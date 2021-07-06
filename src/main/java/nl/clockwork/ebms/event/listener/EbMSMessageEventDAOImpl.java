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
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
abstract class EbMSMessageEventDAOImpl implements EbMSMessageEventDAO
{
	public static class EbMSMessageEventRowMapper implements RowMapper<EbMSMessageEvent>
	{
		@Override
		public EbMSMessageEvent mapRow(ResultSet rs, int nr) throws SQLException
		{
			return new EbMSMessageEvent(rs.getString("message_id"),EbMSMessageEventType.values()[rs.getInt("event_type")]);
		}
	}

	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types)
	{
		val parameters = new ArrayList<Object>();
		return jdbcTemplate.query(
			"select ebms_message_event.message_id, ebms_message_event.event_type" +
			" from ebms_message_event, ebms_message" +
			" where ebms_message_event.processed = 0" +
			" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
			" and ebms_message_event.message_id = ebms_message.message_id" +
			" and ebms_message.message_nr = 0" +
			EbMSDAO.getMessageContextFilter(messageContext,parameters) +
			" order by ebms_message.time_stamp asc",
			parameters.toArray(new Object[0]),
			new EbMSMessageEventRowMapper()
		);
	}

	protected abstract String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr);

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr)
	{
		val parameters = new ArrayList<Object>();
		val messageContextFilter = EbMSDAO.getMessageContextFilter(messageContext,parameters);
		return jdbcTemplate.query(
			getMessageEventsQuery(messageContextFilter,types,maxNr),
			parameters.toArray(new Object[0]),
			new EbMSMessageEventRowMapper()
		);
	}

	@Override
	public String insertEbMSMessageEvent(String messageId, EbMSMessageEventType type)
	{
		jdbcTemplate.update
		(
			"insert into ebms_message_event (" +
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
			"update ebms_message_event" +
			" set processed = 1" +
			" where message_id = ?",
			messageId
		);
	}

	protected String join(EbMSMessageEventType[] array, String delimiter)
	{
		return Stream.of(array).mapToInt(e -> e.getId()).mapToObj(String::valueOf).collect(Collectors.joining(delimiter));
	}
}
