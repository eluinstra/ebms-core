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
package nl.clockwork.mule.ebms.dao.mssql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.Constants.EbMSAcknowledgmentType;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.Constants.EbMSMessageType;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.Acknowledgment;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class EbMSDAOImpl implements EbMSDAO
{
	private static final String defaultDateFormat = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tL";
	protected TransactionTemplate transactionTemplate;
	protected JdbcTemplate jdbcTemplate;
	protected SimpleJdbcTemplate simpleJdbcTemplate;

	public EbMSDAOImpl(PlatformTransactionManager transactionManager, javax.sql.DataSource dataSource)
	{
		transactionTemplate = new TransactionTemplate(transactionManager);
		jdbcTemplate = new JdbcTemplate(dataSource);
		simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public EbMSDAOImpl(TransactionTemplate transactionTemplate, javax.sql.DataSource dataSource)
	{
		this.transactionTemplate = transactionTemplate;
		jdbcTemplate = new JdbcTemplate(dataSource);
		simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	@Override
	public String getDefaultDateFormat()
	{
		return defaultDateFormat;
	}
	
	@Override
	public CollaborationProtocolAgreement getCPA(String cpaId) throws DAOException
	{
		try
		{
			String result = simpleJdbcTemplate.queryForObject(
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
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean exists(String messageId) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForInt(
				"select count(message_id)" +
				" from ebms_message" +
				" where message_id = ?",
				messageId
			) > 0;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public long getIdByMessageId(String messageId) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForLong(
				"select id" +
				" from ebms_message" +
				" where message_id = ?",
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return 0;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public MessageHeader getMessageHeader(long id) throws DAOException
	{
		try
		{
			String result = simpleJdbcTemplate.queryForObject(
				"select message_header" +
				" from ebms_message" +
				" where id = ?",
				String.class,
				id
			);
			return XMLMessageBuilder.getInstance(MessageHeader.class).handle(result);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public MessageHeader getMessageHeader(String messageId) throws DAOException
	{
		try
		{
			String result = simpleJdbcTemplate.queryForObject(
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
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Acknowledgment getAcknowledgment(String messageId) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForObject(
				"select ack_type, ack_header, ack_content" + 
				" from ebms_message" + 
				" where message_id = ?",
				new ParameterizedRowMapper<Acknowledgment>()
				{
					@Override
					public Acknowledgment mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							EbMSAcknowledgmentType acknowledgmentType = EbMSAcknowledgmentType.get(rs.getInt("ack_type"));
							return new Acknowledgment(acknowledgmentType,XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("ack_header")),XMLMessageBuilder.getInstance(acknowledgmentType.equals(EbMSAcknowledgmentType.ACKNOWLEDGMENT) ? nl.clockwork.mule.ebms.model.ebxml.Acknowledgment.class : ErrorList.class).handle(rs.getString("ack_content")));
						}
						catch (JAXBException e)
						{
							throw new SQLException(e);
						}
					}
				},
				messageId
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public List<DataSource> getAttachments(long messageId) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.query(
				"select name, content_type, content" + 
				" from ebms_attachment" + 
				" where ebms_message_id = ?",
				new ParameterizedRowMapper<DataSource>()
				{
					@Override
					public DataSource mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						ByteArrayDataSource result = new ByteArrayDataSource(rs.getBytes("content"),rs.getString("content_type"));
						result.setName(rs.getString("name"));
						return result;
					}
				},
				messageId
			);
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	public EbMSMessage getEbMSMessage(long id) throws DAOException
	{
		return getEbMSMessage(id,false);
	}

	public EbMSMessage getEbMSMessage(final long id, final boolean includeAttachments) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForObject(
				"select message_original, message_header, message_ack_req, message_manifest" + 
				" from ebms_message" + 
				" where id = ?",
				new ParameterizedRowMapper<EbMSMessage>()
				{
					@Override
					public EbMSMessage mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							if (includeAttachments)
								return new EbMSMessage(rs.getBytes("message_original"),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("message_ack_req")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("message_manifest")),getAttachments(id));
							else
								return new EbMSMessage(rs.getBytes("message_original"),XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(AckRequested.class).handle(rs.getString("message_ack_req")),XMLMessageBuilder.getInstance(Manifest.class).handle(rs.getString("message_manifest")),new ArrayList<DataSource>());
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
				},
				id
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return null;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public long insertMessage(final Date timeStamp, final String cpaId, final String conversationId, final String messageId, final EbMSMessageType messageType, final byte[] messageOriginal, final MessageHeader messageHeader, final AckRequested ackRequested, final Manifest manifest, final EbMSMessageStatus status, final List<DataSource> attachments) throws DAOException
	{
		return insertMessage(timeStamp,cpaId,conversationId,messageId,messageType,messageOriginal,messageHeader,ackRequested,manifest,status,attachments,null);
	}

	@Override
	public long insertMessage(final Date timeStamp, final String cpaId, final String conversationId, final String messageId, final EbMSMessageType messageType, final byte[] messageOriginal, final MessageHeader messageHeader, final AckRequested ackRequested, final Manifest manifest, final EbMSMessageStatus status, final List<DataSource> attachments, final Date nextRetryTime) throws DAOException
	{
		try
		{
			return (Long)transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
								new PreparedStatementCreator()
								{
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
													"message_type," +
													"message_original," +
													"message_header," +
													"message_ack_req," +
													"message_manifest," +
													"status," +
													"status_date," +
													"next_retry_time" +
												//") values (?,?,?,(select COALESCE(max(sequence_nr) + 1,0) from ebms_message where cpa_id=? and conversation_id=?),?,?,?,?,?,?,?,GETDATE(),?)",
												") select ?,?,?,COALESCE(max(sequence_nr) + 1,0),?,?,?,?,?,?,?,GETDATE(),? from ebms_message where cpa_id=? and conversation_id=?",
												new String[]{"id"}
											);
											//ps.setDate(1,new java.sql.Date(timeStamp.getTime()));
											ps.setString(1,String.format(defaultDateFormat,timeStamp));
											ps.setString(2,cpaId);
											ps.setString(3,conversationId);
											ps.setString(4,messageId);
											ps.setInt(5,messageType.id());
											ps.setBytes(6,messageOriginal);
											ps.setString(7,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
											ps.setString(8,ackRequested == null ? null : XMLMessageBuilder.getInstance(AckRequested.class).handle(ackRequested));
											ps.setString(9,manifest == null ? null : XMLMessageBuilder.getInstance(Manifest.class).handle(manifest));
											ps.setInt(10,status.id());
											ps.setString(11,nextRetryTime == null ? null : String.format(defaultDateFormat,nextRetryTime));
											ps.setString(12,cpaId);
											ps.setString(13,conversationId);
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
					
							for (DataSource attachment : attachments)
							{
								simpleJdbcTemplate.update
								(
									"insert into ebms_attachment (" +
									"ebms_message_id," +
									"name," +
									"content_type," +
									"content" +
									") values (?,?,?,?)",
									keyHolder.getKey().intValue(),
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentType().split(";")[0],
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							return keyHolder.getKey().longValue();
						}
						catch (Exception e)
						{
							transactionStatus.setRollbackOnly(); 
							throw new RuntimeException(e);
						}
					}
	
				}
			);
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void updateMessage(long id, Date nextRetryTime) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"update ebms_message set" +
				//" nr_retries = nr_retries + 1" +
				" next_retry_time = ?" +
				" where id = ?",
				new Object[]{nextRetryTime == null ? null : String.format(defaultDateFormat,nextRetryTime),id}
			);
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

}
