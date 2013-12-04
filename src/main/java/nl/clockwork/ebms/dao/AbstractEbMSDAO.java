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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.xml.sax.SAXException;

public abstract class AbstractEbMSDAO implements EbMSDAO
{
  protected transient Log logger = LogFactory.getLog(getClass());
	protected ConnectionManager connectionManager;

  public AbstractEbMSDAO(ConnectionManager connectionManager)
	{
		this.connectionManager = connectionManager;
	}

	public abstract String getTimestampFunction();
	
	@Override
	public void executeTransaction(DAOTransactionCallback callback) throws DAOException
	{
		Connection connection = null;
		try
		{
			connection = connectionManager.getConnection(true);
			callback.doInTransaction();
			connectionManager.commit();
		}
		catch (DAOException e)
		{
			if (connection != null)
				connectionManager.rollback();
			throw e;
		}
		catch (RuntimeException e)
		{
			if (connection != null)
				connectionManager.rollback();
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(true);
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
			c = connectionManager.getConnection();
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
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
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
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void insertCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"insert into cpa (" +
					"cpa_id," +
					"cpa" +
				") values (?,?)"
			);
			ps.setString(1,cpa.getCpaid());
			ps.setString(2,XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa));
			ps.executeUpdate();
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public int updateCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"update cpa set" +
				" cpa = ?" +
				" where cpa_id = ?"
			);
			ps.setString(1,XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa));
			ps.setString(2,cpa.getCpaid());
			return ps.executeUpdate();
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public int deleteCPA(String cpaId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"delete from cpa" +
				" where cpa_id = ?"
			);
			ps.setString(1,cpaId);
			return ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"select count(message_id)" +
				" from ebms_message" +
				" where message_id = ?" +
				" and message_nr = 0"
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public EbMSMessageContent getMessageContent(String messageId) throws DAOException
	{
		try
		{
			EbMSMessageContext messageContext = getMessageContext(messageId);
			if (messageContext == null)
				return null;
			List<EbMSAttachment> attachments = getAttachments(messageId);
			List<EbMSDataSource> dataSources = new ArrayList<EbMSDataSource>();
			for (DataSource dataSource : attachments)
				dataSources.add(new EbMSDataSource(dataSource.getName(),dataSource.getContentType(),IOUtils.toByteArray(dataSource.getInputStream())));
			return new EbMSMessageContent(messageContext,dataSources);
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (IOException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public EbMSMessageContext getMessageContext(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessageContext result = null;
			c = connectionManager.getConnection();
			ps = c.prepareStatement(
				"select cpa_id," +
				" from_role," +
				" to_role," +
				" service," +
				" action," +
				" time_stamp," +
				" conversation_id," +
				" message_id," +
				" ref_to_message_id," +
				" sequence_nr" +
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = 0"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
				{
					result = new EbMSMessageContext();
					result.setCpaId(rs.getString("cpa_id"));
					result.setFromRole(rs.getString("from_role"));
					result.setToRole(rs.getString("to_role"));
					result.setService(rs.getString("service"));
					result.setAction(rs.getString("action"));
					result.setTimestamp(rs.getTimestamp("time_stamp"));
					result.setConversationId(rs.getString("conversation_id"));
					result.setMessageId(rs.getString("message_id"));
					result.setRefToMessageId(rs.getString("ref_to_message_id"));
					result.setSequenceNr(rs.getInt("sequence_nr"));
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public EbMSMessageContext getMessageContextByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSMessageContext result = null;
			c = connectionManager.getConnection();
			ps = c.prepareStatement(
				"select cpa_id," +
				" from_role," +
				" to_role," +
				" service," +
				" action," +
				" time_stamp," +
				" conversation_id," +
				" message_id," +
				" ref_to_message_id," +
				" sequence_nr" +
				" from ebms_message" + 
				" where ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')")
			);
			ps.setString(1,refToMessageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
				{
					result = new EbMSMessageContext();
					result.setCpaId(rs.getString("cpa_id"));
					result.setFromRole(rs.getString("from_role"));
					result.setToRole(rs.getString("to_role"));
					result.setService(rs.getString("service"));
					result.setAction(rs.getString("action"));
					result.setTimestamp(rs.getTimestamp("time_stamp"));
					result.setConversationId(rs.getString("conversation_id"));
					result.setMessageId(rs.getString("message_id"));
					result.setRefToMessageId(rs.getString("ref_to_message_id"));
					result.setSequenceNr(rs.getInt("sequence_nr"));
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	@Override
	public EbMSDocument getDocument(String messageId) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSDocument result = null;
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"select content" +
				" from ebms_message" +
				" where message_id = ?" +
				" and message_nr = 0"
			);
			ps.setString(1,messageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = new EbMSDocument(DOMUtils.read(rs.getString("content")),getAttachments(messageId));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new DAOException(e);
		}
		catch (SAXException e)
		{
			throw new DAOException(e);
		}
		catch (IOException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	@Override
	public EbMSDocument getDocumentByRefToMessageId(String refToMessageId, Service service, String...actions) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			EbMSDocument result = null;
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"select content" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')")
			);
			ps.setString(1,refToMessageId);
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result = new EbMSDocument(DOMUtils.read(rs.getString("content")),getAttachments(refToMessageId));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		catch (ParserConfigurationException e)
		{
			throw new DAOException(e);
		}
		catch (SAXException e)
		{
			throw new DAOException(e);
		}
		catch (IOException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
			ps = c.prepareStatement(
				"select status" +
				" from ebms_message" +
				" where message_id = ?" +
				" and message_nr = 0"
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
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"select message_id" +
				" from ebms_message" +
				" where message_nr = 0" +
				" and status = " + status.id() +
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
			connectionManager.close(ps);
			connectionManager.close();
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
			c = connectionManager.getConnection();
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void insertMessage(Date timestamp, EbMSMessage message, EbMSMessageStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection(true);
			ps = c.prepareStatement
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
				") values (?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
				new int[]{5,6}
			);
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
			ps.setString(12,DOMUtils.toString(message.getDocument(),"UTF-8"));
			if (status == null)
				ps.setNull(13,java.sql.Types.INTEGER);
			else
				ps.setInt(13,status.id());
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
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
			ps = c.prepareStatement
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
				") values (?,?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?)",
				new int[]{5,6}
			);
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
			ps.setString(13,DOMUtils.toString(message.getDocument(),"UTF-8"));
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
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
	
	protected void insertAttachments(String messageId, int messageNr, List<EbMSAttachment> attachments) throws SQLException, IOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection(false);
			ps = c.prepareStatement(
				"insert into ebms_attachment (" +
					"message_id," +
					"message_nr," +
					"name," +
					"content_id," +
					"content_type," +
					"content" +
				") values (?,?,?,?,?,?)"
			);
			for (EbMSAttachment attachment : attachments)
			{
				ps.setString(1,messageId);
				ps.setInt(2,messageNr);
				ps.setString(3,attachment.getName());
				ps.setString(4,attachment.getContentId());
				ps.setString(5,attachment.getContentType().split(";")[0].trim());
				ps.setBytes(6,IOUtils.toByteArray(attachment.getInputStream()));
				ps.addBatch();
			}
			if (attachments.size() > 0)
				ps.executeBatch();
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close(false);
		}
	}

	@Override
	public void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"update ebms_message" +
				" set status = ?," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				" and message_nr = 0" +
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void updateMessages(List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"update ebms_message" +
				" set status = " + newStatus.id() + "," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				" and message_nr = 0" +
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public List<EbMSEvent> getLatestEventsByEbMSMessageIdBefore(Date timestamp, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<EbMSEvent> result = new ArrayList<EbMSEvent>();
			c = connectionManager.getConnection();
			ps = c.prepareStatement(
				"select e.message_id, e.time, e.type, e.uri" +
				" from ebms_event e" + 
				" inner join (" +
					"	select message_id, max(time) as time" +
					" from ebms_event" +
					" where status = ?" +
					" and time <= ?" +
					" group by message_id" +
				") l" +
				" on e.message_id = l.message_id" +
				" and e.time = l.time" +
				" order by e.time asc"
			);
			ps.setInt(1,status.id());
			ps.setTimestamp(2,new Timestamp(timestamp.getTime()));
			if (ps.execute())
			{
				ResultSet rs = ps.getResultSet();
				if (rs.next())
					result.add(new EbMSEvent(rs.getString("message_id"),rs.getTimestamp("time"),EbMSEventType.values()[rs.getInt("type")],rs.getString("uri")));
			}
			return result;
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	@Override
	public void insertEvent(String messageId, EbMSEventType type, String uri) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_event (" +
					"message_id," +
					"type," +
					"uri" +
				") values (?,?,?)"
			);
			ps.setString(1,messageId);
			ps.setInt(2,type.id());
			ps.setString(3,uri);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	@Override
	public void insertEvent(EbMSEvent event) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_event (" +
					"message_id," +
					"time," +
					"type," +
					"uri" +
				") values (?,?,?,?)"
			);
			ps.setString(1,event.getMessageId());
			ps.setTimestamp(2,new Timestamp(event.getTime().getTime()));
			ps.setInt(3,event.getType().id());
			ps.setString(4,event.getUri());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void insertEvents(List<EbMSEvent> events) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"insert into ebms_event (" +
					"message_id," +
					"time," +
					"type," +
					"uri" +
				") values (?,?,?,?)"
			);
			for (EbMSEvent event : events)
			{
				ps.setString(1,event.getMessageId());
				ps.setTimestamp(2,new Timestamp(event.getTime().getTime()));
				ps.setInt(3,event.getType().id());
				ps.setString(4,event.getUri());
				ps.addBatch();
			}
			if (events.size() > 0)
				ps.executeBatch();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void updateEvent(Date timestamp, String messageId, EbMSEventStatus status, String errorMessage) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"update ebms_event set" +
				" status = ?," +
				" status_time = " + getTimestampFunction() + "," +
				" error_message = ?" +
				" where message_id = ?" +
				" and time = ?"
			);
			ps.setInt(1,status.id());
			ps.setString(2,errorMessage);
			ps.setString(3,messageId);
			ps.setTimestamp(4,new Timestamp(timestamp.getTime()));
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	@Override
	public void deleteEvents(String messageId, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"delete from ebms_event" +
				" where message_id = ?" +
				" and status = ?"
			);
			ps.setString(1,messageId);
			ps.setInt(2,status.id());
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			throw new DAOException(e);
		}
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
		}
	}

	@Override
	public void deleteEventsBefore(Date timestamp, String messageId, EbMSEventStatus status) throws DAOException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			c = connectionManager.getConnection();
			ps  = c.prepareStatement(
				"delete from ebms_event" +
				" where message_id = ?" +
				" and time < ?" +
				" and status = ?"
			);
			ps.setString(1,messageId);
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
			connectionManager.close(ps);
			connectionManager.close();
		}
	}
	
	protected List<EbMSAttachment> getAttachments(String messageId) throws SQLException
	{
		Connection c = null;
		PreparedStatement ps = null;
		try
		{
			List<EbMSAttachment> result = new ArrayList<EbMSAttachment>();
			c = connectionManager.getConnection();
			ps = c.prepareStatement(
				"select name, content_id, content_type, content" + 
				" from ebms_attachment" + 
				" where message_id = ?" +
				" and message_nr = 0"
			);
			ps.setString(1,messageId);
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
		finally
		{
			connectionManager.close(ps);
			connectionManager.close();
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
	
}
