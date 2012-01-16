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
import java.util.Date;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.bind.JAXBException;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.mule.ebms.Constants;
import nl.clockwork.mule.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.mule.ebms.dao.EbMSDAO;
import nl.clockwork.mule.ebms.model.EbMSAcknowledgment;
import nl.clockwork.mule.ebms.model.EbMSBaseMessage;
import nl.clockwork.mule.ebms.model.EbMSMessage;
import nl.clockwork.mule.ebms.model.EbMSMessageError;
import nl.clockwork.mule.ebms.model.cpp.cpa.CollaborationProtocolAgreement;
import nl.clockwork.mule.ebms.model.cpp.cpa.ReliableMessaging;
import nl.clockwork.mule.ebms.model.ebxml.AckRequested;
import nl.clockwork.mule.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.mule.ebms.model.ebxml.ErrorList;
import nl.clockwork.mule.ebms.model.ebxml.Manifest;
import nl.clockwork.mule.ebms.model.ebxml.MessageHeader;
import nl.clockwork.mule.ebms.model.ebxml.MessageOrder;
import nl.clockwork.mule.ebms.model.ebxml.SyncReply;
import nl.clockwork.mule.ebms.model.xml.xmldsig.ObjectFactory;
import nl.clockwork.mule.ebms.model.xml.xmldsig.SignatureType;
import nl.clockwork.mule.ebms.util.CPAUtils;

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
	public class EbMSBaseMessageParameterizedRowMapper implements ParameterizedRowMapper<EbMSBaseMessage>
	{
		@Override
		public EbMSBaseMessage mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			try
			{
					if (Constants.EBMS_SERVICE.equals(rs.getString("service")) && Constants.EBMS_ERROR.equals(rs.getString("action")))
						return new EbMSMessageError(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(ErrorList.class).handle(rs.getString("content")));
					else if (Constants.EBMS_SERVICE.equals(rs.getString("service")) && Constants.EBMS_ACKNOWLEDGEMENT.equals(rs.getString("action")))
						return new EbMSAcknowledgment(XMLMessageBuilder.getInstance(MessageHeader.class).handle(rs.getString("message_header")),XMLMessageBuilder.getInstance(Acknowledgment.class).handle(rs.getString("content")));
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
	
	public class EbMSMessagePreparedStatement implements PreparedStatementCreator
	{
		private Date timestamp;
		private String cpaId;
		private String conversationId;
		private Long sequenceNr;
		private String messageId;
		private String refToMessageId;
		private String fromRole;
		private String toRole;
		private String service;
		private String action;
		private byte[] original;
		private String signature;
		private String messageHeader;
		private String syncReply;
		private String messageOrder;
		private String ackRequested;
		private String content;
		private EbMSMessageStatus status;

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, String messageId, String refToMessageId, String fromRole, String toRole, String service, String action, String messageHeader, String content)
		{
			this(timestamp,cpaId,conversationId,null,messageId,refToMessageId,fromRole,toRole,service,action,null,null,messageHeader,null,null,null,content,null);
		}

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, String messageId, String refToMessageId, String fromRole, String toRole, String service, String action, String messageHeader, String content, EbMSMessageStatus status)
		{
			this(timestamp,cpaId,conversationId,null,messageId,refToMessageId,fromRole,toRole,service,action,null,null,messageHeader,null,null,null,content,status);
		}

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, Long sequenceNr, String messageId, String refToMessageId, String fromRole, String toRole, String service, String action, String messageHeader, String syncReply, String messageOrder, String ackRequested, String content)
		{
			this(timestamp,cpaId,conversationId,sequenceNr,messageId,refToMessageId,fromRole,toRole,service,action,null,null,messageHeader,syncReply,messageOrder,ackRequested,content,null);
		}
		
		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, Long sequenceNr, String messageId, String refToMessageId, String fromRole, String toRole, String service, String action, byte[] original, String signature, String messageHeader, String syncReply, String messageOrder, String ackRequested, String content, EbMSMessageStatus status)
		{
			this.timestamp = timestamp;
			this.cpaId = cpaId;
			this.conversationId = conversationId;
			this.sequenceNr = sequenceNr;
			this.messageId = messageId;
			this.refToMessageId = refToMessageId;
			this.fromRole = fromRole;
			this.toRole = toRole;
			this.service = service;
			this.action = action;
			this.original = original;
			this.signature = signature;
			this.messageHeader = messageHeader;
			this.syncReply = syncReply;
			this.messageOrder = messageOrder;
			this.ackRequested = ackRequested;
			this.content = content;
			this.status = status;
		}

		public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
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
					"service," +
					"action," +
					"original," +
					"signature," +
					"message_header," +
					"sync_reply," +
					"message_order," +
					"ack_requested," +
					"content," +
					"status," +
					"status_time" +
					") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : NOW) + ")",
				//new String[]{"id"}
				new int[]{1}
			);
			//ps.setDate(1,new java.sql.Date(timestamp.getTime()));
			ps.setString(1,String.format(DATE_FORMAT,timestamp));
			ps.setString(2,cpaId);
			ps.setString(3,conversationId);
			if (sequenceNr == null)
				ps.setNull(4,java.sql.Types.BIGINT);
			else
				ps.setLong(4,sequenceNr);
			ps.setString(5,messageId);
			ps.setString(6,refToMessageId);
			ps.setString(7,fromRole);
			ps.setString(8,toRole);
			ps.setString(9,service);
			ps.setString(10,action);
			ps.setBytes(11,original);
			ps.setString(12,signature);
			ps.setString(13,messageHeader);
			ps.setString(14,syncReply);
			ps.setString(15,messageOrder);
			ps.setString(16,ackRequested);
			ps.setString(17,content);
			if (status == null)
				ps.setNull(18,java.sql.Types.INTEGER);
			else
				ps.setInt(18,status.id());
			//ps.setString(19,status == null ? null : String.format(DATE_FORMAT,timestamp));
			return ps;
		}
	}

	private static final String DATE_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tL";
	private static final String NOW = "GETDATE()";
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
	
	private List<DataSource> getAttachments(long messageId) throws DAOException
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

	@Override
	public EbMSBaseMessage getEbMSMessage(final long id) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where id = ?",
				new EbMSBaseMessageParameterizedRowMapper(),
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
	public void insertMessage(final EbMSMessage message) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getValue(),
											message.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(message.getMessageHeader()),
											XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()),
											XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()),
											XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()),
											XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest())
									),
									keyHolder
							);
					
							for (DataSource attachment : message.getAttachments())
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
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}

							Date sendTime = (Date)timestamp.clone();
							CollaborationProtocolAgreement cpa = getCPA(message.getMessageHeader().getCPAId());
							ReliableMessaging rm = CPAUtils.getReliableMessaging(cpa,message.getMessageHeader());
							if (rm != null)
								for (int i = 0; i < rm.getRetries().intValue(); i++)
								{
									simpleJdbcTemplate.update
									(
										"insert into ebms_send_event (" +
										"ebms_message_id," +
										"time" +
										") values (?,?)",
										keyHolder.getKey().intValue(),
										String.format(DATE_FORMAT,(Date)sendTime.clone())
									);
									rm.getRetryInterval().addTo(sendTime);
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
	public void insertMessage(final EbMSMessageError messageError, final EbMSMessageStatus status) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											messageError.getMessageHeader().getCPAId(),
											messageError.getMessageHeader().getConversationId(),
											messageError.getMessageHeader().getMessageData().getMessageId(),
											messageError.getMessageHeader().getMessageData().getRefToMessageId(),
											messageError.getMessageHeader().getFrom().getRole(),
											messageError.getMessageHeader().getTo().getRole(),
											messageError.getMessageHeader().getService().getValue(),
											messageError.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageError.getMessageHeader()),
											XMLMessageBuilder.getInstance(ErrorList.class).handle(messageError.getErrorList()),
											status
									),
									keyHolder
							);

							long id = getIdByMessageId(messageError.getMessageHeader().getMessageData().getRefToMessageId());
							simpleJdbcTemplate.update
							(
								"update ebms_send_event" +
								" set status=1, status_time=GETDATE()" +
								" where ebms_message_id=? and status=0",
								id
							);

							return null;
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
	public void insertMessage(final EbMSAcknowledgment acknowledgment, final EbMSMessageStatus status) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											acknowledgment.getMessageHeader().getCPAId(),
											acknowledgment.getMessageHeader().getConversationId(),
											acknowledgment.getMessageHeader().getMessageData().getMessageId(),
											acknowledgment.getMessageHeader().getMessageData().getRefToMessageId(),
											acknowledgment.getMessageHeader().getFrom().getRole(),
											acknowledgment.getMessageHeader().getTo().getRole(),
											acknowledgment.getMessageHeader().getService().getValue(),
											acknowledgment.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(acknowledgment.getMessageHeader()),
											XMLMessageBuilder.getInstance(Acknowledgment.class).handle(acknowledgment.getAcknowledgment()),
											status
									),
									keyHolder
							);

							long id = getIdByMessageId(acknowledgment.getMessageHeader().getMessageData().getRefToMessageId());
							simpleJdbcTemplate.update
							(
								"update ebms_send_event" +
								" set status=1, status_time=GETDATE()" +
								" where ebms_message_id=? and status=0",
								id
							);

							return null;
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
	public void insertMessage(final EbMSMessage message, final EbMSMessageStatus status, final EbMSMessageError messageError) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getValue(),
											message.getMessageHeader().getAction(),
											message.getOriginal(),
											XMLMessageBuilder.getInstance(SignatureType.class).handle(new ObjectFactory().createSignature(message.getSignature())),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(message.getMessageHeader()),
											XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()),
											XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()),
											XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()),
											XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest()),
											status
									),
									keyHolder
							);
					
							for (DataSource attachment : message.getAttachments())
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
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											messageError.getMessageHeader().getCPAId(),
											messageError.getMessageHeader().getConversationId(),
											messageError.getMessageHeader().getMessageData().getMessageId(),
											messageError.getMessageHeader().getMessageData().getRefToMessageId(),
											messageError.getMessageHeader().getFrom().getRole(),
											messageError.getMessageHeader().getTo().getRole(),
											messageError.getMessageHeader().getService().getValue(),
											messageError.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageError.getMessageHeader()),
											XMLMessageBuilder.getInstance(ErrorList.class).handle(messageError.getErrorList())
									),
									keyHolder
							);
					
							simpleJdbcTemplate.update
							(
								"insert into ebms_send_event (" +
								"ebms_message_id," +
								"time" +
								") values (?,?)",
								keyHolder.getKey().intValue(),
								String.format(DATE_FORMAT,timestamp)
							);

							return null;
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
	public void insertMessage(final EbMSMessage message, final EbMSMessageStatus status, final EbMSAcknowledgment acknowledgment) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallback()
				{
	
					@Override
					public Object doInTransaction(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getValue(),
											message.getMessageHeader().getAction(),
											message.getOriginal(),
											XMLMessageBuilder.getInstance(SignatureType.class).handle(new ObjectFactory().createSignature(message.getSignature())),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(message.getMessageHeader()),
											XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()),
											XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()),
											XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()),
											XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest()),
											status
									),
									keyHolder
							);
					
							for (DataSource attachment : message.getAttachments())
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
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											acknowledgment.getMessageHeader().getCPAId(),
											acknowledgment.getMessageHeader().getConversationId(),
											acknowledgment.getMessageHeader().getMessageData().getMessageId(),
											acknowledgment.getMessageHeader().getMessageData().getRefToMessageId(),
											acknowledgment.getMessageHeader().getFrom().getRole(),
											acknowledgment.getMessageHeader().getTo().getRole(),
											acknowledgment.getMessageHeader().getService().getValue(),
											acknowledgment.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(acknowledgment.getMessageHeader()),
											XMLMessageBuilder.getInstance(Acknowledgment.class).handle(acknowledgment.getAcknowledgment())
									),
									keyHolder
							);
					
							simpleJdbcTemplate.update
							(
								"insert into ebms_send_event (" +
								"ebms_message_id," +
								"time" +
								") values (?,?)",
								keyHolder.getKey().intValue(),
								String.format(DATE_FORMAT,timestamp)
							);

							return null;
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
	public List<String> getMessageIds(int maxNr) throws DAOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EbMSBaseMessage getEbMSMessage(String messageId) throws DAOException
	{
		try
		{
			return simpleJdbcTemplate.queryForObject(
				"select id, service, action, message_header, ack_requested, content" + 
				" from ebms_message" + 
				" where message_id = ?",
				new EbMSBaseMessageParameterizedRowMapper(),
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
	public void processMessage(String messageId) throws DAOException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processMessages(List<String> messageIds) throws DAOException
	{
		// TODO Auto-generated method stub
		
	}

}
