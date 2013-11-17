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
package nl.clockwork.ebms.dao.spring;

import java.io.IOException;
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
import nl.clockwork.ebms.Constants.EbMSEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.XMLMessageBuilder;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.DAOTransactionCallback;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.io.IOUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.AckRequested;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.ErrorList;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Manifest;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageOrder;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.SyncReply;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3._2000._09.xmldsig.SignatureType;

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
					return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("ack_requested")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("content")),getAttachments(rs.getLong("id")));
				else if (EbMSAction.MESSAGE_ERROR.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(ErrorList.class).handle(rs.getString("content")));
				else if (EbMSAction.ACKNOWLEDGMENT.action().equals(rs.getString("action")))
					return new EbMSMessage(XMLMessageBuilder.getInstance(SignatureType.class).handle(rs.getString("signature")),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(Acknowledgment.class).handle(rs.getString("content")));
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
	
	public AbstractEbMSDAO(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate)
	{
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
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
				" order by cpa_id asc",
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
			return jdbcTemplate.update
			(
				"insert into cpa (" +
					"cpa_id," +
					"cpa" +
				") values (?,?)",
				cpa.getCpaid(),
				XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa)
			) > 0;
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
	public boolean updateCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"update cpa set" +
				" cpa = ?" +
				" where cpa_id = ?",
				XMLMessageBuilder.getInstance(CollaborationProtocolAgreement.class).handle(cpa),
				cpa.getCpaid()
			) > 0;
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
				" where message_id = ?" +
				" and message_nr = 0",
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
				" where message_id = ?" +
				" and message_nr = 0",
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
	public Long getMessageId(String refToMessageId, Service service, String...actions) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForLong(
				"select id" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")"),
				refToMessageId
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
	public EbMSMessage getMessage(String refToMessageId, Service service, String...actions) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select id, service, action, signature, message_header, ack_requested, content" +
				" from ebms_message" +
				" where ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in (" + join(actions,",") + ")"),
				new EbMSMessageParameterizedRowMapper(),
				refToMessageId
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
				" where message_id = ?" +
				" and message_nr = 0",
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
				"select id, service, action, signature, message_header, ack_requested, content" + 
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
					" where message_id = ?" +
					" and message_nr = 0",
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
			return null;
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public List<EbMSEvent> getLatestEventsByEbMSMessageIdBefore(Date timestamp, EbMSEventStatus status) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select e.ebms_message_id, e.time, e.type" +
				" from ebms_event e" + 
				" inner join (" +
					"	select ebms_message_id, max(time) as time" +
					" from ebms_event" +
					" where status = ?" +
					" and time <= ?" +
					" group by ebms_message_id" +
				") l" +
				" on e.ebms_message_id = l.ebms_message_id" +
				" and e.time = l.time" +
				" order by e.time asc",
				new ParameterizedRowMapper<EbMSEvent>()
				{
					@Override
					public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						return new EbMSEvent(rs.getLong("ebms_message_id"),rs.getTimestamp("time"),EbMSEventType.values()[rs.getInt("type")]);
					}
				},
				status.id(),
				timestamp
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void updateEvent(Date timestamp, Long ebMSMessageId, EbMSEventStatus status, String errorMessage) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"update ebms_event set" +
				" status = ?," +
				" status_time = " + getTimestampFunction() + "," +
				" error_message = ?" +
				" where ebms_message_id = ?" +
				" and time = ?",
				status.id(),
				errorMessage,
				ebMSMessageId,
				timestamp
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void deleteEventsBefore(Date timestamp, Long ebMSMessageId, EbMSEventStatus status) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"delete from ebms_event" +
				" where ebms_message_id = ?" +
				" and time < ?" +
				" and status = ?",
				ebMSMessageId,
				timestamp,
				status.id()
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
													"time_to_live," +
													"from_role," +
													"to_role," +
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
									attachment.getName(),
									attachment.getContentId(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							return keyHolder.getKey().longValue();
						}
						catch (IOException e)
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public long insertDuplicateMessage(final Date timestamp, final EbMSMessage message) throws DAOException
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
													"message_nr," +
													"ref_to_message_id," +
													"time_to_live," +
													"from_role," +
													"to_role," +
													"service," +
													"action," +
													"signature," +
													"message_header," +
													"sync_reply," +
													"message_order," +
													"ack_requested," +
													"content" +
												") values (?,?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?,?,?,?,?,?)",
												//new String[]{"id"}
												new int[]{1}
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
											ps.setString(13,XMLMessageBuilder.getInstance(SignatureType.class).handle(new JAXBElement<SignatureType>(new QName("http://www.w3.org/2000/09/xmldsig#","Signature"),SignatureType.class,message.getSignature())));
											ps.setString(14,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
											ps.setString(15,XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()));
											ps.setString(16,XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()));
											ps.setString(17,XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()));
											ps.setString(18,getContent(message));
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
									attachment.getName(),
									attachment.getContentId(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							return keyHolder.getKey().longValue();
						}
						catch (IOException e)
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
		catch (DataAccessException e)
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
		return null;
	}

	@Override
	public void updateMessageStatus(Long ebMSMessageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
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
				ebMSMessageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertEvent(long id, EbMSEventType type) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into ebms_event (" +
					"ebms_message_id," +
					"type" +
				") values (?,?)",
				id,
				type.id()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void insertEvent(EbMSEvent event) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into ebms_event (" +
					"ebms_message_id," +
					"time," +
					"type" +
				") values (?,?,?)",
				event.getEbMSMessageId(),
				//String.format(getDateFormat(),event.getTime())
				event.getTime(),
				event.getType().id()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertEvents(final List<EbMSEvent> events) throws DAOException
	{
		try
		{
			jdbcTemplate.batchUpdate
			(
				"insert into ebms_event (" +
					"ebms_message_id," +
					"time," +
					"type" +
				") values (?,?,?)",
				new BatchPreparedStatementSetter()
				{
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException
					{
						ps.setLong(1,events.get(i).getEbMSMessageId());
						//ps.setTimestamp(2,String.format(getDateFormat(),events.get(i).getTime()));
						ps.setTimestamp(2,new Timestamp(events.get(i).getTime().getTime()));
						ps.setInt(3,events.get(i).getType().id());
					}
					
					@Override
					public int getBatchSize()
					{
						return events.size();
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
	public void deleteEvents(Long ebMSMessageId, EbMSEventStatus status) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"delete from ebms_event" +
				" where ebms_message_id = ?" +
				" and status = ?",
				ebMSMessageId,
				status.id()
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
					" where message_nr = 0" +
					" and status = " + status.id() +
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
				"select id, service, action, signature, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = 0",
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
				" and message_nr = 0" +
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
					" and message_nr = 0" +
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
