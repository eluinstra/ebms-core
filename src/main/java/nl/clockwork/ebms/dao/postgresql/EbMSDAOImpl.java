/*******************************************************************************
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
 ******************************************************************************/
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
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;

import org.apache.commons.io.IOUtils;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public EbMSDAOImpl(ConnectionManager connectionManager)
	{
		super(connectionManager);
	}

//	@Override
//	public String getDateFormat()
//	{
//		return "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL";
//	}

	@Override
	public String getTimestampFunction()
	{
		return "NOW()";
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
	protected PreparedStatement getInsertMessagePreparedStatement(Connection connection, EbMSMessageStatus status) throws SQLException
	{
		return connection.prepareStatement
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
				"service_type," +
				"service," +
				"action," +
				"content," +
				"status," +
				"status_time" +
			") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")" +
			" returning message_id, message_nr"
		);
	}

	@Override
	protected PreparedStatement getInsertDuplicateMessagePreparedStatement(Connection connection) throws SQLException
	{
		return connection.prepareStatement
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
				"service_type," +
				"service," +
				"action," +
				"content" +
			") values (?,?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?,?)" +
			" returning message_id, message_nr"
		);
	}

	@Override
	public void insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection(true);
			ps = getInsertMessagePreparedStatement(c,status);
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
			ps.setString(10,messageHeader.getService().getType());
			ps.setString(11,messageHeader.getService().getValue());
			ps.setString(12,messageHeader.getAction());
			ps.setString(13,DOMUtils.toString(message.getDocument(),"UTF-8"));
			if (status == null)
				ps.setNull(14,java.sql.Types.INTEGER);
			else
				ps.setInt(14,status.id());
			//ps.setString(15,status == null ? null : String.format(getDateFormat(),timestamp));
			//ps.setTimestamp(15,status == null ? null : new Timestamp(timestamp.getTime()));
			//ps.setObject(15,status == null ? null : timestamp,Types.TIMESTAMP);
			//ps.setObject(15,status == null ? null : timestamp);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
			{
				String messageId = rs.getString(1);
				int messageNr = rs.getInt(2);
				connectionManager.close(ps);
				ps  = c.prepareStatement(
					"insert into ebms_attachment (" +
						"message_id," +
						"message_nr," +
						"name," +
						"content_id," +
						"content_type," +
						"content" +
					") values (?,?,?,?,?,?)"
				);
				for (EbMSAttachment attachment : message.getAttachments())
				{
					ps.setString(1,messageId);
					ps.setInt(2,messageNr);
					ps.setString(3,attachment.getName());
					ps.setString(4,attachment.getContentId());
					ps.setString(5,attachment.getContentType().split(";")[0].trim());
					ps.setBytes(6,IOUtils.toByteArray(attachment.getInputStream()));
					ps.addBatch();
				}
				if (message.getAttachments().size() > 0)
					ps.executeBatch();
				connectionManager.commit();
			}
			else
			{
				connectionManager.rollback();
				throw new DAOException("No key found!");
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
			connectionManager.close(ps);
			connectionManager.close(true);
		}
	}

	@Override
	public void insertDuplicateMessage(Date timestamp, EbMSMessage message) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection(true);
			ps = getInsertDuplicateMessagePreparedStatement(c);
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
			ps.setString(11,messageHeader.getService().getType());
			ps.setString(12,messageHeader.getService().getValue());
			ps.setString(13,messageHeader.getAction());
			ps.setString(14,DOMUtils.toString(message.getDocument(),"UTF-8"));
			ResultSet rs = ps.executeQuery();
			if (rs.next())
			{
				String messageId = rs.getString(1);
				int messageNr = rs.getInt(2);
				connectionManager.close(ps);
				ps  = c.prepareStatement(
					"insert into ebms_attachment (" +
						"message_id," +
						"message_nr," +
						"name," +
						"content_id," +
						"content_type," +
						"content" +
					") values (?,?,?,?,?,?)"
				);
				for (EbMSAttachment attachment : message.getAttachments())
				{
					ps.setString(1,messageId);
					ps.setInt(2,messageNr);
					ps.setString(3,attachment.getName());
					ps.setString(4,attachment.getContentId());
					ps.setString(5,attachment.getContentType().split(";")[0].trim());
					ps.setBytes(6,IOUtils.toByteArray(attachment.getInputStream()));
					ps.addBatch();
				}
				if (message.getAttachments().size() > 0)
					ps.executeBatch();
				connectionManager.commit();
			}
			else
			{
				connectionManager.rollback();
				throw new DAOException("No key found!");
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
			connectionManager.close(ps);
			connectionManager.close(true);
		}
	}
	
}
