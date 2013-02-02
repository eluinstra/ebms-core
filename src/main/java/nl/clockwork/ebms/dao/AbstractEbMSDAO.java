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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.Constants.EbMSMessageType;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
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
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class AbstractEbMSDAO implements EbMSDAO
{
	public class EbMSMessageParameterizedRowMapper implements ParameterizedRowMapper<EbMSMessage>
	{
		@Override
		public EbMSMessage mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			try
			{
					if (EbMSMessageType.MESSAGE_ERROR.action().getService().getValue().equals(rs.getString("service")) && EbMSMessageType.MESSAGE_ERROR.action().getAction().equals(rs.getString("action")))
						return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(ErrorList.class).handle(rs.getString("content")));
					else if (EbMSMessageType.ACKNOWLEDGMENT.action().getService().getValue().equals(rs.getString("service")) && EbMSMessageType.ACKNOWLEDGMENT.action().getAction().equals(rs.getString("action")))
						return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(Acknowledgment.class).handle(rs.getString("content")));
					else
						return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("ack_requested")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("content")),getAttachments(rs.getLong("id")));
			}
			catch (JAXBException e)
			{
				throw new SQLException(e);
			}
			catch (DAOException e)
			{
				throw new SQLException(e);
			}
		}
	}
	
	protected TransactionTemplate transactionTemplate;
	protected JdbcTemplate jdbcTemplate;

	//public abstract String getDateFormat();
	public abstract String getTimestampFunction();
	
	public AbstractEbMSDAO(PlatformTransactionManager transactionManager, javax.sql.DataSource dataSource)
	{
		transactionTemplate = new TransactionTemplate(transactionManager);
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public AbstractEbMSDAO(TransactionTemplate transactionTemplate, javax.sql.DataSource dataSource)
	{
		this.transactionTemplate = transactionTemplate;
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean existsCPA(String cpaId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForInt(
				"select count(cpa_id)" +
				" from cpa" +
				" where cpa_id = ?",
				cpaId
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException
	{
		try
		{
			String result = jdbcTemplate.queryForObject(
				"select cpa" +
				" from cpa" +
				" where cpa_id = ?",
				String.class,
				cpaId
			);
			return XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(result);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public List<String> getCPAIds() throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForList(
					"select cpa_id" +
					" from cpa" +
					" order by cpa_id desc",
					String.class
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean insertCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into cpa (" +
					"cpa_id," +
					"cpa" +
				") values (?,?)",
				cpa.getCpaid(),
				XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa)
			);
			return true;
		}
		catch (DataAccessException e)
		{
			return false;
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"update cpa set" +
				" cpa = ?" +
				" where cpa_id = ?",
				XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa),
				cpa.getCpaid()
			);
			return true;
		}
		catch (DataAccessException e)
		{
			return false;
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean deleteCPA(String cpaId) throws DAOException
	{
		try
		{
			return
				jdbcTemplate.update
				(
					"delete from cpa" +
					" where cpa_id = ?",
					cpaId
				) > 0
			;
		}
		catch (DataAccessException e)
		{
			return false;
		}
	}

	@Override
	public boolean existsMessage(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForInt(
				"select count(message_id)" +
				" from ebms_message" +
				" where message_id = ?",
				messageId
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Long getEbMSMessageId(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForLong(
				"select id" +
				" from ebms_message" +
				" where message_id = ?",
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Long getEbMSMessageResponseId(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForLong(
				"select id" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				" and service = '" + Constants.EBMS_SERVICE_URI + "'" +
				" and (action = '" + EbMSMessageType.MESSAGE_ERROR.action().getAction() + "'"  +
				" or action = '" + EbMSMessageType.ACKNOWLEDGMENT.action().getAction() + "')",
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public EbMSMessage getEbMSMessageResponse(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				" and service = '" + Constants.EBMS_SERVICE_URI + "'" +
				" and (action = '" + EbMSMessageType.MESSAGE_ERROR.action().getAction() + "'"  +
				" or action = '" + EbMSMessageType.ACKNOWLEDGMENT.action().getAction() + "')",
				new EbMSMessageParameterizedRowMapper(),
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public MessageHeader getMessageHeader(String messageId) throws DAOException
	{
		try
		{
			String result = jdbcTemplate.queryForObject(
				"select message_header" + 
				" from ebms_message" + 
				" where message_id = ?",
				String.class,
				messageId
			);
			return XMLMessageBuilder.getInstance(MessageHeader.class).handle(result);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
		catch (JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	private List<EbMSAttachment> getAttachments(long messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select name, content_id, content_type, content" + 
				" from ebms_attachment" + 
				" where ebms_message_id = ?",
				new ParameterizedRowMapper<EbMSAttachment>()
				{
					@Override
					public EbMSAttachment mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						ByteArrayDataSource dataSource = new ByteArrayDataSource(rs.getBytes("content"),rs.getString("content_type"));
						dataSource.setName(rs.getString("name"));
						return new EbMSAttachment(dataSource,rs.getString("content_id"));
					}
				},
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public EbMSMessage getMessage(final long id) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where id = ?",
				new EbMSMessageParameterizedRowMapper(),
				id
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public EbMSMessageStatus getMessageStatus(String messageId) throws DAOException
	{
		try
		{
			return EbMSMessageStatus.get(
				jdbcTemplate.queryForObject(
					"select status" +
					" from ebms_message" +
					" where message_id = ?",
					new ParameterizedRowMapper<Integer>()
					{
						@Override
						public Integer mapRow(ResultSet rs, int rowNum) throws SQLException
						{
							return rs.getObject("status") == null ? null : rs.getInt("status");
						}
					},
					messageId
				)
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return EbMSMessageStatus.NOT_RECOGNIZED;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public List<EbMSSendEvent> selectEventsForSending(GregorianCalendar timestamp) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select ebms_message_id, max(time) as time" +
				" from ebms_send_event" +
				" where status = 0 " +
				//" and time <= " + getTimestampFunction() +
				" and time <= ?" +
				" group by ebms_message_id",
				new ParameterizedRowMapper<EbMSSendEvent>()
				{
					@Override
					public EbMSSendEvent mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						return new EbMSSendEvent(rs.getLong("ebms_message_id"),rs.getDate("time"));
					}
				},
				timestamp.getTime()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void deleteEventsForSending(final GregorianCalendar timestamp, final Long id) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						jdbcTemplate.update(
							"update ebms_send_event set" +
							" status = 1," +
							" status_time=NOW()" +
							" where ebms_message_id = ?" +
							" and time = ?",
							id,
							timestamp.getTime()
						);
						jdbcTemplate.update(
							"delete from ebms_send_event" +
							" where ebms_message_id=?" +
							" and time < ?" +
							"and status=0",
							id,
							timestamp.getTime()
						);
					}
				}
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void deleteExpiredEvents(GregorianCalendar timestamp, Long id) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"delete from ebms_send_event" +
				" where ebms_message_id=?" +
				" and time < ?" +
				"and status=0",
				id,
				timestamp.getTime()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertMessage(final Date timestamp, final EbMSMessage message, final List<EbMSSendEvent> sendEvents) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						try
						{
							long id = insertMessage1(timestamp,message,null);
							insertSendEvents(id,sendEvents);
						}
						catch (Exception e)
						{
							throw new DAOException(e);
						}
					}
	
				}
			);
		}
		catch (TransactionException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void insertMessage(final Date timestamp, final EbMSMessage message, final EbMSMessageStatus refToMessageStatus) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						try
						{
							insertMessage1(timestamp,message,null);
							Long id = getEbMSMessageId(message.getMessageHeader().getMessageData().getRefToMessageId());
							if (id != null)
							{
								deleteSendEvents(id);
								updateMessageStatus(id,refToMessageStatus);
							}
						}
						catch (Exception e)
						{
							throw new DAOException(e);
						}
					}
	
				}
			);
		}
		catch (TransactionException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertMessage(final Date timestamp, final EbMSMessage request, final EbMSMessageStatus status, final EbMSMessage response, final EbMSSendEvent sendEvent) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						try
						{
							insertMessage(timestamp,request,status);
							long id = insertMessage1(timestamp,response,null);
							insertSendEvent(id,sendEvent);
						}
						catch (Exception e)
						{
							throw new DAOException(e);
						}
					}
	
				}
			);
		}
		catch (TransactionException e)
		{
			throw new DAOException(e);
		}
	}
	
	protected long insertMessage1(final Date timestamp, final EbMSMessage message, final EbMSMessageStatus status) throws InvalidDataAccessApiUsageException, DataAccessException, IOException
	{
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(
			new PreparedStatementCreator()
			{
				
				@Override
				public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
				{
					try
					{
						PreparedStatement ps = connection.prepareStatement
						(
							"insert into ebms_message (" +
								"time_stamp," +
								"cpa_id," +
								"conversation_id," +
								"sequence_nr," +
								"message_id," +
								"ref_to_message_id," +
								"from_role," +
								"to_role," +
								"service_type," +
								"service," +
								"action," +
								"original," +
								"signature," +
								"message_header," +
								"sync_reply," +
								"message_order," +
								"ack_requested," +
								"error_list," +
								"acknowledgment," +
								"manifest," +
								"status_request," +
								"status_response," +
								"status," +
								"status_time" +
							") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
							//new String[]{"id"}
							new int[]{1}
						);
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
						ps.setString(7,messageHeader.getFrom().getRole());
						ps.setString(8,messageHeader.getTo().getRole());
						ps.setString(9,messageHeader.getService().getType());
						ps.setString(10,messageHeader.getService().getValue());
						ps.setString(11,messageHeader.getAction());
						ps.setBytes(12,message.getOriginal());
						ps.setString(13,XMLMessageBuilder.getInstance(SignatureType.class).handle(message.getSignature()));
						ps.setString(14,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
						ps.setString(15,XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()));
						ps.setString(16,XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()));
						ps.setString(17,XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()));
						ps.setString(18,XMLMessageBuilder.getInstance(ErrorList.class).handle(message.getErrorList()));
						ps.setString(19,XMLMessageBuilder.getInstance(Acknowledgment.class).handle(message.getAcknowledgment()));
						ps.setString(20,XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest()));
						ps.setString(21,XMLMessageBuilder.getInstance(StatusRequest.class).handle(message.getStatusRequest()));
						ps.setString(22,XMLMessageBuilder.getInstance(StatusResponse.class).handle(message.getStatusResponse()));
						if (status == null)
							ps.setNull(23,java.sql.Types.INTEGER);
						else
							ps.setInt(23,status.id());
						//ps.setString(24,status == null ? null : String.format(getDateFormat(),timestamp));
						//ps.setTimestamp(24,status == null ? null : new Timestamp(timestamp.getTime()));
						//ps.setObject(24,status == null ? null : timestamp,Types.TIMESTAMP);
						//ps.setObject(24,status == null ? null : timestamp);
						return ps;
					}
					catch (JAXBException e)
					{
						throw new SQLException(e);
					}
				}
			},
			keyHolder
		);

		for (EbMSAttachment attachment : message.getAttachments())
		{
			jdbcTemplate.update
			(
				"insert into ebms_attachment (" +
					"ebms_message_id," +
					"name," +
					"content_id," +
					"content_type," +
					"content" +
				") values (?,?,?,?,?)",
				keyHolder.getKey().longValue(),
				attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
				attachment.getContentId(),
				attachment.getContentType().split(";")[0].trim(),
				IOUtils.toByteArray(attachment.getInputStream())
			);
		}
		
		return keyHolder.getKey().longValue();
	}

	protected void updateMessageStatus(Long id, EbMSMessageStatus status)
	{
		jdbcTemplate.update
		(
			"update ebms_message set status=?" +
			" where id=?" +
			" and status is null",
			id,
			status.id()
		);
	}

	protected void insertSendEvent(long id, EbMSSendEvent sendEvent)
	{
		if (sendEvent != null)
			jdbcTemplate.update
			(
				"insert into ebms_send_event (" +
					"ebms_message_id," +
					"time" +
				") values (?,?)",
				id,
				//String.format(getDateFormat(),sendEvent.getTime())
				sendEvent.getTime()
			);
	}

	protected void insertSendEvents(long id, List<EbMSSendEvent> sendEvents)
	{
		List<Object[]> events = new ArrayList<Object[]>();
		for (EbMSSendEvent sendEvent : sendEvents)
		{
			//events.add(new Object[]{keyHolder.getKey().longValue(),String.format(getDateFormat(),sendEvent.getTime())});
			events.add(new Object[]{id,sendEvent.getTime()});
		}
		jdbcTemplate.batchUpdate
		(
			"insert into ebms_send_event (" +
				"ebms_message_id," +
				"time" +
			") values (?,?)",
			events
		);
	}

	protected void deleteSendEvents(Long id)
	{
		jdbcTemplate.update
		(
			"delete from ebms_send_event" +
			" where ebms_message_id = ?" +
			" and status = 0",
			id
		);
	}

	@Override
	public void insertSendEvent(long id) throws DAOException
	{
		jdbcTemplate.update
		(
			"insert into ebms_send_event (" +
				"ebms_message_id" +
			") values (?)",
			id
		);
	}
	
	public String getMessageContextFilter(EbMSMessageContext messageContext, List<Object> parameters)
	{
		StringBuffer result = new StringBuffer();
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
			{
				parameters.add(messageContext.getCpaId());
				result.append(" and cpa_id = ?");
			}
			if (messageContext.getFromRole() != null)
			{
				parameters.add(messageContext.getFromRole());
				result.append(" and from_role = ?");
			}
			if (messageContext.getToRole() != null)
			{
				parameters.add(messageContext.getToRole());
				result.append(" and to_role = ?");
			}
			if (messageContext.getServiceType() != null)
			{
				parameters.add(messageContext.getServiceType());
				result.append(" and service_type = ?");
			}
			if (messageContext.getService() != null)
			{
				parameters.add(messageContext.getService());
				result.append(" and service = ?");
			}
			if (messageContext.getAction() != null)
			{
				parameters.add(messageContext.getAction());
				result.append(" and action = ?");
			}
			if (messageContext.getConversationId() != null)
			{
				parameters.add(messageContext.getConversationId());
				result.append(" and conversation_id = ?");
			}
			if (messageContext.getMessageId() != null)
			{
				parameters.add(messageContext.getMessageId());
				result.append(" and message_id = ?");
			}
			if (messageContext.getRefToMessageId() != null)
			{
				parameters.add(messageContext.getRefToMessageId());
				result.append(" and ref_to_message_id = ?");
			}
			if (messageContext.getSequenceNr() != null)
			{
				parameters.add(messageContext.getSequenceNr());
				result.append(" and sequence_nr = ?");
			}
		}
		return result.toString();
	}
	
	@Override
	public List<String> getReceivedMessageIds(EbMSMessageContext messageContext) throws DAOException
	{
		try
		{
			List<Object> parameters = new ArrayList<Object>();
			return jdbcTemplate.queryForList(
					"select message_id" +
					" from ebms_message" +
					" where status = " + EbMSMessageStatus.RECEIVED.id() +
					getMessageContextFilter(messageContext,parameters) +
					" order by time_stamp asc",
					parameters.toArray(new Object[0]),
					String.class
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	public abstract String getReceivedMessageIdsQuery(String messageContextFilter, int maxNr);

	@Override
	public List<String> getReceivedMessageIds(EbMSMessageContext messageContext, int maxNr) throws DAOException
	{
		try
		{
			//TODO improve: add maxNr to parameters???
			List<Object> parameters = new ArrayList<Object>();
			String messageContextFilter = getMessageContextFilter(messageContext,parameters);
			return jdbcTemplate.queryForList(
					getReceivedMessageIdsQuery(messageContextFilter,maxNr),
					parameters.toArray(new Object[0]),
					String.class
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public EbMSMessage getMessage(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where message_id = ?",// and status=" + EbMSMessageStatus.RECEIVED.id(),
				new EbMSMessageParameterizedRowMapper(),
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void processReceivedMessage(String messageId) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"update ebms_message" +
				" set status = " + EbMSMessageStatus.PROCESSED.id() + "," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				" and status = " + EbMSMessageStatus.RECEIVED.id(),
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void processReceivedMessages(final List<String> messageIds) throws DAOException
	{
		try
		{
			List<Object[]> ids = new ArrayList<Object[]>();
			for (String messageId : messageIds)
				ids.add(new Object[]{messageId});
			jdbcTemplate.batchUpdate(
					"update ebms_message" +
					" set status = " + EbMSMessageStatus.PROCESSED.id() + "," +
					" status_time = " + getTimestampFunction() +
					" where message_id = ?" +
					" and status = " + EbMSMessageStatus.RECEIVED.id(),
					ids
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

}
