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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
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
import nl.clockwork.ebms.model.ebxml.Service;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
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
				if (!Constants.EBMS_SERVICE_URI.equals(rs.getString("service")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("ack_requested")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("content")),getAttachments(rs.getLong("id")));
				else if (EbMSAction.MESSAGE_ERROR.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(ErrorList.class).handle(rs.getString("content")));
				else if (EbMSAction.ACKNOWLEDGMENT.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(Acknowledgment.class).handle(rs.getString("content")));
				else if (EbMSAction.STATUS_REQUEST.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),null,XMLMessageBuilder.getInstance(StatusRequest.class).handle(rs.getString("content")));
				else if (EbMSAction.STATUS_RESPONSE.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(StatusResponse.class).handle(rs.getString("content")));
				else if (EbMSAction.PING.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")));
				else if (EbMSAction.PONG.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")));
				else
					return null;
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
	public boolean existsMessage(String messageId, Service service, String[] actions) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForInt(
				"select count(message_id)" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				(service.getType() == null ? "" : " and serviceType = '" + service.getType() + "'") +
				(service.getValue() == null ? "" : " and service = '" + service.getValue() + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")"),
				messageId
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	private String join(String[] array, String delimiter)
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
	
	@Override
	public Long getMessageId(String messageId) throws DAOException
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
	public Long getMessageId(String messageId, Service service, String[] actions) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForLong(
				"select id" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				(service.getType() == null ? "" : " and serviceType = '" + service.getType() + "'") +
				(service.getValue() == null ? "" : " and service = '" + service.getValue() + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")"),
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
	public EbMSMessage getMessage(String messageId, Service service, String[] actions) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				(service.getType() == null ? "" : " and serviceType = '" + service.getType() + "'") +
				(service.getValue() == null ? "" : " and service = '" + service.getValue() + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")"),
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
	public void executeTransaction(final DAOTransactionCallback callback)
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						callback.doInTransaction();
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
	public List<EbMSSendEvent> selectEventsForSending(Date timestamp) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select ebms_message_id, max(time) as time" +
				" from ebms_send_event" +
				" where status = 0" +
				//" and time <= " + getTimestampFunction() +
				" and time <= ?" +
				" group by ebms_message_id",
				new ParameterizedRowMapper<EbMSSendEvent>()
				{
					@Override
					public EbMSSendEvent mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						return new EbMSSendEvent(rs.getLong("ebms_message_id"),rs.getTimestamp("time"));
					}
				},
				timestamp
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void updateSentEvent(Date timestamp, Long id) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"update ebms_send_event set" +
				" status = 1," +
				" status_time = NOW()" +
				" where ebms_message_id = ?" +
				" and time = ?",
				id,
				timestamp
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void deleteEventsBefore(Date timestamp, Long id, EbMSEventStatus status) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"delete from ebms_send_event" +
				" where ebms_message_id = ?" +
				" and time < ?" +
				"and status = ?",
				id,
				timestamp,
				status.ordinal()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public long insertMessage(final Date timestamp, final EbMSMessage message, final EbMSMessageStatus status) throws DAOException
	{
		try
		{
			return transactionTemplate.execute(
				new TransactionCallback<Long>()
				{
					@Override
					public Long doInTransaction(TransactionStatus arg0)
					{
						try
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
													"signature," +
													"message_header," +
													"sync_reply," +
													"message_order," +
													"ack_requested," +
													"content," +
													"status," +
													"status_time" +
												") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
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
											ps.setString(12,XMLMessageBuilder.getInstance(SignatureType.class).handle(new JAXBElement<SignatureType>(new QName("http://www.w3.org/2000/09/xmldsig#","Signature"),SignatureType.class,message.getSignature())));
											ps.setString(13,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
											ps.setString(14,XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()));
											ps.setString(15,XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()));
											ps.setString(16,XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()));
											ps.setString(17,getContent(message));
											if (status == null)
												ps.setNull(18,java.sql.Types.INTEGER);
											else
												ps.setInt(18,status.id());
											//ps.setString(19,status == null ? null : String.format(getDateFormat(),timestamp));
											//ps.setTimestamp(19,status == null ? null : new Timestamp(timestamp.getTime()));
											//ps.setObject(19,status == null ? null : timestamp,Types.TIMESTAMP);
											//ps.setObject(19,status == null ? null : timestamp);
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

	@Override
	public void updateMessageStatus(Long id, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"update ebms_message" +
				" set status = ?," + 
				" status_time = " + getTimestampFunction() +
				" where id = ?" +
				(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id()),
				newStatus.id(),
				id
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertSendEvent(long id) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into ebms_send_event (" +
					"ebms_message_id" +
				") values (?)",
				id
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void insertSendEvent(long id, EbMSSendEvent sendEvent) throws DAOException
	{
		try
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertSendEvents(long id, List<EbMSSendEvent> sendEvents) throws DAOException
	{
		try
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void deleteSendEvents(Long id) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"delete from ebms_send_event" +
				" where ebms_message_id = ?" +
				" and status = 0",
				id
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	protected String getMessageContextFilter(EbMSMessageContext messageContext, List<Object> parameters)
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
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException
	{
		try
		{
			List<Object> parameters = new ArrayList<Object>();
			return jdbcTemplate.queryForList(
					"select message_id" +
					" from ebms_message" +
					" where status = " + status.id() +
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

	public abstract String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr);

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException
	{
		try
		{
			List<Object> parameters = new ArrayList<Object>();
			String messageContextFilter = getMessageContextFilter(messageContext,parameters);
			return jdbcTemplate.queryForList(
					getMessageIdsQuery(messageContextFilter,status,maxNr),
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
				" where message_id = ?",
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
	public void updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"update ebms_message" +
				" set status = ?," +
				" status_time = " + getTimestampFunction() +
				" where message_id = ?" +
				(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id()),
				newStatus.id(),
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void updateMessages(final List<String> messageIds, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		try
		{
			List<Object[]> ids = new ArrayList<Object[]>();
			for (String messageId : messageIds)
				ids.add(new Object[]{messageId});
			jdbcTemplate.batchUpdate(
					"update ebms_message" +
					" set status = " + newStatus.id() + "," +
					" status_time = " + getTimestampFunction() +
					" where message_id = ?" +
					(oldStatus == null ? " and status is null" : " and status = " + oldStatus.id()),
					ids
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

}
