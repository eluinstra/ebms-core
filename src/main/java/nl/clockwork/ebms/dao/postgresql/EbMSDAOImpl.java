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
package nl.clockwork.ebms.dao.postgresql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.dao.ConnectionManager;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(ConnectionManager connectionManager)
	{
		super(connectionManager);
	}

	@Override
	public String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where message_nr = 0" +
		" and status=" + status.id() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}

	@Override
	public void insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException
	{
		Connection c = null;
		try
		{
			c = connectionManager.getConnection(true);
			try (PreparedStatement ps = c.prepareStatement
			(
				"insert into ebms_message (" +
					"time_stamp," +
					"cpa_id," +
					"conversation_id," +
					"sequence_nr," +
					"message_id," +
					"ref_to_message_id," +
					"time_to_live," +
					"from_role," +
					"to_role," +
					"service," +
					"action," +
					"content," +
					"status," +
					"status_time" +
				") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)" +
				" returning message_id, message_nr"
			))
			{
				ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
				MessageHeader messageHeader = message.getMessageHeader();
				ps.setString(2,messageHeader.getCPAId());
				ps.setString(3,messageHeader.getConversationId());
				if (message.getMessageOrder() == null || message.getMessageOrder().getSequenceNumber() == null)
					ps.setNull(4,java.sql.Types.BIGINT);
				else
					ps.setLong(4,message.getMessageOrder().getSequenceNumber().getValue().longValue());
				ps.setString(5,messageHeader.getMessageData().getMessageId());
				ps.setString(6,messageHeader.getMessageData().getRefToMessageId());
				ps.setTimestamp(7,messageHeader.getMessageData().getTimeToLive() == null ? null : new Timestamp(messageHeader.getMessageData().getTimeToLive().toGregorianCalendar().getTimeInMillis()));
				ps.setString(8,messageHeader.getFrom().getRole());
				ps.setString(9,messageHeader.getTo().getRole());
				ps.setString(10,EbMSMessageUtils.toString(messageHeader.getService()));
				ps.setString(11,messageHeader.getAction());
				ps.setString(12,DOMUtils.toString(message.getMessage(),"UTF-8"));
				if (status == null)
				{
					ps.setNull(13,java.sql.Types.INTEGER);
					ps.setNull(14,java.sql.Types.TIMESTAMP);
				}
				else
				{
					ps.setInt(13,status.id());
					ps.setTimestamp(14,new Timestamp(timestamp.getTime()));
				}
				ResultSet rs = ps.executeQuery();
				if (rs.next())
				{
					insertAttachments(rs.getString(1),rs.getInt(2),message.getAttachments());
					connectionManager.commit();
				}
				else
				{
					connectionManager.rollback();
					throw new DAOException("No key found!");
				}
			}
		}
		catch (SQLException | IOException | TransformerException e)
		{
			connectionManager.rollback();
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(true);
		}
	}

	@Override
	public void insertDuplicateMessage(Date timestamp, EbMSMessage message) throws DAOException
	{
		Connection c = null;
		try
		{
			c = connectionManager.getConnection(true);
			try (PreparedStatement ps = c.prepareStatement
			(
				"insert into ebms_message (" +
					"time_stamp," +
					"cpa_id," +
					"conversation_id," +
					"sequence_nr," +
					"message_id," +
					"message_nr," +
					"ref_to_message_id," +
					"time_to_live," +
					"from_role," +
					"to_role," +
					"service," +
					"action," +
					"content" +
				") values (?,?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?)" +
				" returning message_id, message_nr"
			))
			{
				ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
				MessageHeader messageHeader = message.getMessageHeader();
				ps.setString(2,messageHeader.getCPAId());
				ps.setString(3,messageHeader.getConversationId());
				if (message.getMessageOrder() == null || message.getMessageOrder().getSequenceNumber() == null)
					ps.setNull(4,java.sql.Types.BIGINT);
				else
					ps.setLong(4,message.getMessageOrder().getSequenceNumber().getValue().longValue());
				ps.setString(5,messageHeader.getMessageData().getMessageId());
				ps.setString(6,messageHeader.getMessageData().getMessageId());
				ps.setString(7,messageHeader.getMessageData().getRefToMessageId());
				ps.setTimestamp(8,messageHeader.getMessageData().getTimeToLive() == null ? null : new Timestamp(messageHeader.getMessageData().getTimeToLive().toGregorianCalendar().getTimeInMillis()));
				ps.setString(9,messageHeader.getFrom().getRole());
				ps.setString(10,messageHeader.getTo().getRole());
				ps.setString(11,EbMSMessageUtils.toString(messageHeader.getService()));
				ps.setString(12,messageHeader.getAction());
				ps.setString(13,DOMUtils.toString(message.getMessage(),"UTF-8"));
				ResultSet rs = ps.executeQuery();
				if (rs.next())
				{
					insertAttachments(rs.getString(1),rs.getInt(2),message.getAttachments());
					connectionManager.commit();
				}
				else
				{
					connectionManager.rollback();
					throw new DAOException("No key found!");
				}
			}
		}
		catch (SQLException e)
		{
			connectionManager.rollback();
			throw new DAOException(e);
		}
		catch (IOException e)
		{
			connectionManager.rollback();
			throw new DAOException(e);
		}
		catch (TransformerException e)
		{
			connectionManager.rollback();
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(true);
		}
	}
	
}
