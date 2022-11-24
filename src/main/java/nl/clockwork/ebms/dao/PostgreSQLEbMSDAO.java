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
package nl.clockwork.ebms.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;

import lombok.val;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.util.DOMUtils;

class PostgreSQLEbMSDAO extends AbstractEbMSDAO
{
	public class KeyExtractor implements ResultSetExtractor<KeyHolder>
	{
		@Override
		public KeyHolder extractData(ResultSet rs) throws SQLException
		{
			if (rs.next())
			{
				val keys = new HashMap<String,Object>();
				keys.put("message_id",rs.getString("message_id"));
				keys.put("message_nr",rs.getInt("message_nr"));
				return new KeyHolder()
				{
					@Override
					public Map<String,Object> getKeys() throws InvalidDataAccessApiUsageException
					{
						return keys;
					}

					@Override
					public List<Map<String,Object>> getKeyList()
					{
						return Arrays.asList(keys);
					}

					@Override
					public Number getKey() throws InvalidDataAccessApiUsageException
					{
						throw new InvalidDataAccessApiUsageException("");
					}

					@Override
					public <T> T getKeyAs(Class<T> keyType) throws InvalidDataAccessApiUsageException
					{
						throw new InvalidDataAccessApiUsageException("");
					}
				};
			}
			else
				return null;
		}
	}

	public PostgreSQLEbMSDAO(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate)
	{
		super(transactionTemplate,jdbcTemplate);
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where message_nr = 0" +
		" and status=" + status.getId() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}

	@Override
	public String insertMessage(final Instant timestamp, final Instant persistTime, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments, final EbMSMessageStatus status)
	{
		val keyHolder = jdbcTemplate.query(c ->
		{
			try
			{
				val ps = c.prepareStatement
				(
					"insert into ebms_message (" +
						"time_stamp," +
						"cpa_id," +
						"conversation_id," +
						"message_id," +
						"ref_to_message_id," +
						"time_to_live," +
						"from_party_id," +
						"from_role," +
						"to_party_id," +
						"to_role," +
						"service," +
						"action," +
						"content," +
						"status," +
						"status_time," +
						"persist_time" +
					") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" +
					" returning message_id, message_nr"
				);
				ps.setTimestamp(1,Timestamp.from(timestamp));
				val messageHeader = message.getMessageHeader();
				ps.setString(2,messageHeader.getCPAId());
				ps.setString(3,messageHeader.getConversationId());
				ps.setString(4,messageHeader.getMessageData().getMessageId());
				ps.setString(5,messageHeader.getMessageData().getRefToMessageId());
				ps.setTimestamp(6,messageHeader.getMessageData().getTimeToLive() == null ? null : Timestamp.from(messageHeader.getMessageData().getTimeToLive()));
				ps.setString(7,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
				ps.setString(8,messageHeader.getFrom().getRole());
				ps.setString(9,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
				ps.setString(10,messageHeader.getTo().getRole());
				ps.setString(11,EbMSMessageUtils.toString(messageHeader.getService()));
				ps.setString(12,messageHeader.getAction());
				ps.setString(13,DOMUtils.toString(document,"UTF-8"));
				ps.setObject(14,status != null ? status.getId() : null,java.sql.Types.INTEGER);
				ps.setTimestamp(15,status != null ? Timestamp.from(timestamp) : null);
				ps.setTimestamp(16,persistTime != null ? Timestamp.from(persistTime) : null);
				return ps;
			}
			catch (TransformerException e)
			{
				throw new SQLException(e);
			}
		},
		new KeyExtractor());
		insertAttachments(keyHolder,attachments);
		return (String)keyHolder.getKeys().get("message_id");
	}
	}
