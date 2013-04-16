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
package nl.clockwork.ebms.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.sql.DataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractEbMSDAO implements EbMSDAO
{
  protected transient Log logger = LogFactory.getLog(getClass());
	protected DataSource dataSource;
	//public abstract String getDateFormat();
	public abstract String getTimestampFunction();
	
	public AbstractEbMSDAO(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public void executeTransaction(DAOTransactionCallback callback) throws DAOException
	{
		Connection connection = null;
		try
		{
			connection = getConnection(true);
			callback.doInTransaction();
			commit(connection);
		}
		catch (DAOException e)
		{
			if (connection != null)
				rollback(connection);
			throw e;
		}
		catch (RuntimeException e)
		{
			if (connection != null)
				rollback(connection);
			throw new DAOException(e);
		}
		finally
		{
			close(connection);
		}
	}

	@Override
	public boolean existsCPA(String cpaId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			boolean result = false;
			c = getConnection();
			ps  = c.prepareStatement(
				"select count(cpa_id)" +
				" from cpa" +
				" where cpa_id = ?"
			);
			ps.setString(1,cpaId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = rs.getInt(1) > 0;
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			CollaborationProtocolAgreement result = null;
			c = getConnection();
			ps  = c.prepareStatement(
				"select cpa" +
				" from cpa" +
				" where cpa_id = ?"
			);
			ps.setString(1,cpaId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(rs.getString("cpa"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public List<String> getCPAIds() throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<String> result = new ArrayList<String>();
			c = getConnection();
			ps  = c.prepareStatement(
				"select cpa_id" +
				" from cpa" +
				" order by cpa_id asc"
			);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				while (rs.next())
					result.add(rs.getString("cpa_id"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public boolean insertCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"insert into cpa (" +
					"cpa_id," +
					"cpa" +
				") values (?,?)"
			);
			ps.setString(1,cpa.getCpaid());
			ps.setString(2,XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa));
			return ps.executeUpdate() > 0;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"update cpa set" +
				" cpa = ?" +
				" where cpa_id = ?"
			);
			ps.setString(1,XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa));
			ps.setString(2,cpa.getCpaid());
			return ps.executeUpdate() > 0;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public boolean deleteCPA(String cpaId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"delete from cpa" +
				" where cpa_id = ?"
			);
			ps.setString(1,cpaId);
			return ps.executeUpdate() > 0;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public boolean existsMessage(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			boolean result = false;
			c = getConnection();
			ps  = c.prepareStatement(
				"select count(message_id)" +
				" from ebms_message" +
				" where message_id = ?"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = rs.getInt(1) > 0;
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public Long getMessageId(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			Long result = null;
			c = getConnection();
			ps  = c.prepareStatement(
				"select id" +
				" from ebms_message" +
				" where message_id = ?"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = rs.getLong("id");
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public Long getMessageId(String messageId, Service service, String...actions) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			Long result = null;
			c = getConnection();
			ps  = c.prepareStatement(
				"select id" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				(service.getType() == null ? "" : " and serviceType = '" + service.getType() + "'") +
				(service.getValue() == null ? "" : " and service = '" + service.getValue() + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")")
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = rs.getLong("id");
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public EbMSMessage getMessage(String messageId, Service service, String...actions) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessage result = null;
			c = getConnection();
			ps  = c.prepareStatement(
				"select id, service, action, signature, message_header, ack_requested, content" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				(service.getType() == null ? "" : " and serviceType = '" + service.getType() + "'") +
				(service.getValue() == null ? "" : " and service = '" + service.getValue() + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")")
		);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = getEbMSMessage(rs);
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public MessageHeader getMessageHeader(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			MessageHeader result = null;
			c = getConnection();
			ps = c.prepareStatement(
				"select message_header" + 
				" from ebms_message" + 
				" where message_id = ?"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public EbMSMessage getMessage(long id) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessage result = null;
			c = getConnection();
			ps = c.prepareStatement(
				"select id, service, action, signature, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where id = ?"
			);
			ps.setLong(1,id);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = getEbMSMessage(rs);
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public EbMSMessageStatus getMessageStatus(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessageStatus result = null;
			c = getConnection();
			ps = c.prepareStatement(
				"select status" +
				" from ebms_message" +
				" where message_id = ?"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = EbMSMessageStatus.get(rs.getObject("status") == null ? null : rs.getInt("status"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public List<EbMSSendEvent> getLatestEventsByEbMSMessageIdBefore(Date timestamp, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<EbMSSendEvent> result = new ArrayList<EbMSSendEvent>();
			c = getConnection();
			ps = c.prepareStatement(
				"select ebms_message_id, max(time) as time" +
				" from ebms_send_event" +
				" where status = ?" +
				" and time <= ?" +
				" group by ebms_message_id" +
				" order by time asc"
			);
			ps.setInt(1,status.id());
			ps.setTimestamp(2,new Timestamp(timestamp.getTime()));
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result.add(new EbMSSendEvent(rs.getLong("ebms_message_id"),rs.getTimestamp("time")));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public void updateSendEvent(Date timestamp, Long ebMSMessageId, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"update ebms_send_event set" +
				" status = ?," +
				" status_time = " + getTimestampFunction() +
				" where ebms_message_id = ?" +
				" and time = ?"
			);
			ps.setInt(1,status.id());
			ps.setLong(2,ebMSMessageId);
			ps.setTimestamp(3,new Timestamp(timestamp.getTime()));
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public void deleteEventsBefore(Date timestamp, Long ebMSMessageId, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"delete from ebms_send_event" +
				" where ebms_message_id = ?" +
				" and time < ?" +
				" and status = ?"
			);
			ps.setLong(1,ebMSMessageId);
			ps.setTimestamp(2,new Timestamp(timestamp.getTime()));
			ps.setInt(3,status.id());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
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
				"signature," +
				"message_header," +
				"sync_reply," +
				"message_order," +
				"ack_requested," +
				"content," +
				"status," +
				"status_time" +
			") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
			Statement.RETURN_GENERATED_KEYS
		);
	}

	@Override
	public long insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			//c = getConnection(true);
			c = getConnection();
			ps = getInsertMessagePreparedStatement(c,status);
			//ps.setDate(1,new java.sql.Date(timestamp.getTime()));
			//ps.setString(1,String.format(getDateFormat(),timestamp));
			ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
			//ps.setObject(1,timestamp,Types.TIMESTAMP);
			//ps.setObject(1,timestamp);
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
			ps.setString(13,XMLMessageBuilder.getInstance(SignatureType.class).handle(new JAXBElement<SignatureType>(new QName("http://www.w3.org/2000/09/xmldsig#","Signature"),SignatureType.class,message.getSignature())));
			ps.setString(14,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
			ps.setString(15,XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()));
			ps.setString(16,XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()));
			ps.setString(17,XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()));
			ps.setString(18,getContent(message));
			if (status == null)
				ps.setNull(19,java.sql.Types.INTEGER);
			else
				ps.setInt(19,status.id());
			//ps.setString(20,status == null ? null : String.format(getDateFormat(),timestamp));
			//ps.setTimestamp(20,status == null ? null : new Timestamp(timestamp.getTime()));
			//ps.setObject(20,status == null ? null : timestamp,Types.TIMESTAMP);
			//ps.setObject(20,status == null ? null : timestamp);
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next())
			{
				long key = rs.getLong(1);
				ps.close();
				ps  = c.prepareStatement(
					"insert into ebms_attachment (" +
					"ebms_message_id," +
					"name," +
					"content_id," +
					"content_type," +
					"content" +
					") values (?,?,?,?,?)"
				);
				for (EbMSAttachment attachment : message.getAttachments())
				{
					ps.setLong(1,key);
					ps.setString(2,attachment.getName());
					ps.setString(3,attachment.getContentId());
					ps.setString(4,attachment.getContentType().split(";")[0].trim());
					ps.setBytes(5,IOUtils.toByteArray(attachment.getInputStream()));
					ps.addBatch();
				}
				if (message.getAttachments().size() > 0)
					ps.executeBatch();
				//commit(c);
				return key;
			}
			else
			{
				//rollback(c);
				throw new DAOException("No key found!");
			}
		}
		catch (SQLException e)
		{
			//rollback(c);
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			//rollback(c);
			throw new DAOException(e);
		}
		catch (IOException e)
		{
			//rollback(c);
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void updateMessageStatus(Long ebMSMessageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"update ebms_message" +
				" set status = ?," + 
				" status_time = " + getTimestampFunction() +
				" where id = ?" +
				(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id())
			);
			ps.setInt(1,newStatus.id());
			ps.setLong(2,ebMSMessageId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void insertSendEvent(long ebMSMessageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_send_event (" +
					"ebms_message_id" +
				") values (?)"
			);
			ps.setLong(1,ebMSMessageId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}
	
	@Override
	public void insertSendEvent(EbMSSendEvent sendEvent) throws DAOException
	{
		//if (sendEvent != null)
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_send_event (" +
					"ebms_message_id," +
					"time" +
				") values (?,?)"
			);
			ps.setLong(1,sendEvent.getEbMSMessageId());
			//ps.setTimestamp(2,String.format(getDateFormat(),sendEvent.getTime()));
			ps.setTimestamp(2,new Timestamp(sendEvent.getTime().getTime()));
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void insertSendEvents(List<EbMSSendEvent> sendEvents) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_send_event (" +
					"ebms_message_id," +
					"time" +
				") values (?,?)"
			);
			for (EbMSSendEvent sendEvent : sendEvents)
			{
				ps.setLong(1,sendEvent.getEbMSMessageId());
				ps.setTimestamp(2,new Timestamp(sendEvent.getTime().getTime()));
				ps.addBatch();
			}
			if (sendEvents.size() > 0)
				ps.executeBatch();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void deleteSendEvents(Long ebMSMessageId, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"delete from ebms_send_event" +
				" where ebms_message_id = ?" +
				" and status = ?"
			);
			ps.setLong(1,ebMSMessageId);
			ps.setInt(2,status.id());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<String> result = new ArrayList<String>();
			c = getConnection();
			ps  = c.prepareStatement(
				"select message_id" +
				" from ebms_message" +
				" where status = " + status.id() +
				addMessageContextFilter(messageContext) +
				" order by time_stamp asc"
			);
			addMessageContextFilter(messageContext,ps);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				while (rs.next())
					result.add(rs.getString("message_id"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	public abstract String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr);

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<String> result = new ArrayList<String>();
			c = getConnection();
			String messageContextFilter = addMessageContextFilter(messageContext);
			ps = c.prepareStatement(
				getMessageIdsQuery(messageContextFilter,status,maxNr)
			);
			addMessageContextFilter(messageContext,ps);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				while (rs.next())
					result.add(rs.getString("message_id"));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public EbMSMessage getMessage(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessage result = null;
			c = getConnection();
			ps = c.prepareStatement(
				"select id, service, action, signature, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where message_id = ?"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = getEbMSMessage(rs);
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"update ebms_message" +
				" set status = ?," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id())
			);
			ps.setInt(1,newStatus.id());
			ps.setString(2,messageId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	@Override
	public void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = getConnection();
			ps  = c.prepareStatement(
				"update ebms_message" +
				" set status = " + newStatus.id() + "," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id())
			);
			for (String messageId : messageIds)
			{
				ps.setString(1,messageId);
				ps.addBatch();
			}
			if (messageIds.size() > 0)
				ps.executeBatch();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	private List<EbMSAttachment> getAttachments(long messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<EbMSAttachment> result = new ArrayList<EbMSAttachment>();
			c = getConnection();
			ps = c.prepareStatement(
				"select name, content_id, content_type, content" + 
				" from ebms_attachment" + 
				" where ebms_message_id = ?"
			);
			ps.setLong(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				while (rs.next())
				{
					ByteArrayDataSource dataSource = new ByteArrayDataSource(rs.getBytes("content"),rs.getString("content_type"));
					dataSource.setName(rs.getString("name"));
					result.add(new EbMSAttachment(dataSource,rs.getString("content_id")));
				}
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			close(ps);
			close(c);
		}
	}

	protected String addMessageContextFilter(EbMSMessageContext messageContext) throws SQLException
	{
		StringBuffer result = new StringBuffer();
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
				result.append(" and cpa_id = ?");
			if (messageContext.getFromRole() != null)
				result.append(" and from_role = ?");
			if (messageContext.getToRole() != null)
				result.append(" and to_role = ?");
			if (messageContext.getServiceType() != null)
				result.append(" and service_type = ?");
			if (messageContext.getService() != null)
				result.append(" and service = ?");
			if (messageContext.getAction() != null)
				result.append(" and action = ?");
			if (messageContext.getConversationId() != null)
				result.append(" and conversation_id = ?");
			if (messageContext.getMessageId() != null)
				result.append(" and message_id = ?");
			if (messageContext.getRefToMessageId() != null)
				result.append(" and ref_to_message_id = ?");
			if (messageContext.getSequenceNr() != null)
				result.append(" and sequence_nr = ?");
		}
		return result.toString();
	}
	
	protected void addMessageContextFilter(EbMSMessageContext messageContext, PreparedStatement ps) throws SQLException
	{
		if (messageContext != null)
		{
			int parameterIndex = 0;
			if (messageContext.getCpaId() != null)
				ps.setString(parameterIndex++,messageContext.getCpaId());
			if (messageContext.getFromRole() != null)
				ps.setString(parameterIndex++,messageContext.getFromRole());
			if (messageContext.getToRole() != null)
				ps.setString(parameterIndex++,messageContext.getToRole());
			if (messageContext.getServiceType() != null)
				ps.setString(parameterIndex++,messageContext.getServiceType());
			if (messageContext.getService() != null)
				ps.setString(parameterIndex++,messageContext.getService());
			if (messageContext.getAction() != null)
				ps.setString(parameterIndex++,messageContext.getAction());
			if (messageContext.getConversationId() != null)
				ps.setString(parameterIndex++,messageContext.getConversationId());
			if (messageContext.getMessageId() != null)
				ps.setString(parameterIndex++,messageContext.getMessageId());
			if (messageContext.getRefToMessageId() != null)
				ps.setString(parameterIndex++,messageContext.getRefToMessageId());
			if (messageContext.getSequenceNr() != null)
				ps.setObject(parameterIndex++,messageContext.getSequenceNr());
		}
	}
	
	protected EbMSMessage getEbMSMessage(ResultSet rs) throws DAOException, SQLException, JAXBException
	{
		if (!Constants.EBMS_SERVICE_URI.equals(rs.getString("service")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("ack_requested")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("content")),getAttachments(rs.getLong("id")));
		else if (EbMSAction.MESSAGE_ERROR.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(ErrorList.class).handle(rs.getString("content")));
		else if (EbMSAction.ACKNOWLEDGMENT.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(Acknowledgment.class).handle(rs.getString("content")));
		else if (EbMSAction.STATUS_REQUEST.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),null,XMLMessageBuilder.getInstance(StatusRequest.class).handle(rs.getString("content")));
		else if (EbMSAction.STATUS_RESPONSE.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(StatusResponse.class).handle(rs.getString("content")));
		else if (EbMSAction.PING.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")));
		else if (EbMSAction.PONG.action().equals(rs.getString("action")))
			return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")));
		else
			return null;
	}

	protected String getContent(EbMSMessage message) throws JAXBException
	{
		if (!Constants.EBMS_SERVICE_URI.equals(message.getMessageHeader().getService().getValue()))
			return XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest());
		else if (EbMSAction.MESSAGE_ERROR.action().equals(message.getMessageHeader().getAction()))
			return XMLMessageBuilder.getInstance(ErrorList.class).handle(message.getErrorList());
		else if (EbMSAction.ACKNOWLEDGMENT.action().equals(message.getMessageHeader().getAction()))
			return XMLMessageBuilder.getInstance(Acknowledgment.class).handle(message.getAcknowledgment());
		else if (EbMSAction.STATUS_REQUEST.action().equals(message.getMessageHeader().getAction()))
			return XMLMessageBuilder.getInstance(StatusRequest.class).handle(message.getStatusRequest());
		else if (EbMSAction.STATUS_RESPONSE.action().equals(message.getMessageHeader().getAction()))
			return XMLMessageBuilder.getInstance(StatusResponse.class).handle(message.getStatusResponse());
		return null;
	}

	protected Connection getConnection() throws DAOException
	{
		return getConnection(false);
	}

	protected Connection getConnection(boolean startTransaction) throws DAOException
	{
		try
		{
			Connection connection = ConnectionManager.get();
			if (connection == null)
				connection = dataSource.getConnection();
			if (startTransaction)
			{
				connection.setAutoCommit(false);
				ConnectionManager.set(connection);
			}
			return connection;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
	}

	protected void commit(Connection connection) throws DAOException
	{
		try
		{
			connection.commit();
			connection.setAutoCommit(true);
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
	}

	protected void rollback(Connection connection) throws DAOException
	{
		try
		{
			connection.rollback();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
	}

	protected void close(Connection connection) throws DAOException
	{
		close(connection,false);
	}
	
	protected void close(Connection connection, boolean endTransaction) throws DAOException
	{
		try
		{
			if (connection != null)
				if ((!endTransaction && ConnectionManager.get() == null) || (endTransaction && ConnectionManager.get() != null))
				connection.close();
			if (endTransaction)
				ConnectionManager.unset();
		}
		catch (SQLException e)
		{
			//throw new DAOException(e);
			logger.warn("",e);
		}
	}

	protected void close(Statement ps) throws DAOException
	{
		try
		{
			if (ps != null)
				ps.close();
		}
		catch (SQLException e)
		{
			//throw new DAOException(e);
			logger.warn("",e);
		}
	}

	protected String join(String[] array, String delimiter)
	{
		StringBuffer result = new StringBuffer();
		if (array.length > 0)
		{
			for (String s : array)
				result.append("'").append(s).append("'").append(delimiter);
			result.deleteCharAt(result.length() - 1);
		}
		return result.toString();
	}
	
}
