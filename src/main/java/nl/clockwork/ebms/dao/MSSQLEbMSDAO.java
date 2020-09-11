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
package nl.clockwork.ebms.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.w3c.dom.Document;

import com.querydsl.sql.SQLQueryFactory;

import lombok.val;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.util.DOMUtils;

class MSSQLEbMSDAO extends MySQLEbMSDAO
{
	public MSSQLEbMSDAO(JdbcTemplate jdbcTemplate, SQLQueryFactory queryFactory)
	{
		super(jdbcTemplate,queryFactory);
	}

	@Override
	public long insertDuplicateMessage(final Instant timestamp, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try
						{
							val ps = connection.prepareStatement
							(
								"insert into ebms_message (" +
									"time_stamp," +
									"cpa_id," +
									"conversation_id," +
									"message_id," +
									"message_nr," +
									"ref_to_message_id," +
									"time_to_live," +
									"from_party_id," +
									"from_role," +
									"to_party_id," +
									"to_role," +
									"service," +
									"action," +
									"content" +
								") values (?,?,?,?,(select max(message_nr) + 1 as nr from ebms_message where message_id = ?),?,?,?,?,?,?,?,?,?)",
								new int[]{1}
							);
							ps.setTimestamp(1,Timestamp.from(timestamp));
							val messageHeader = message.getMessageHeader();
							ps.setString(2,messageHeader.getCPAId());
							ps.setString(3,messageHeader.getConversationId());
							ps.setString(4,messageHeader.getMessageData().getMessageId());
							ps.setString(5,messageHeader.getMessageData().getMessageId());
							ps.setString(6,messageHeader.getMessageData().getRefToMessageId());
							ps.setTimestamp(7,messageHeader.getMessageData().getTimeToLive() == null ? null : Timestamp.from(messageHeader.getMessageData().getTimeToLive()));
							ps.setString(8,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
							ps.setString(8,messageHeader.getFrom().getRole());
							ps.setString(10,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
							ps.setString(9,messageHeader.getTo().getRole());
							ps.setString(10,EbMSMessageUtils.toString(messageHeader.getService()));
							ps.setString(11,messageHeader.getAction());
							ps.setString(12,DOMUtils.toString(document,"UTF-8"));
							return ps;
						}
						catch (TransformerException e)
						{
							throw new SQLException(e);
						}
					}
				},
				keyHolder
			);
			insertAttachments(keyHolder.getKey().longValue(),attachments);
			return keyHolder.getKeyList().size();
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}
}
