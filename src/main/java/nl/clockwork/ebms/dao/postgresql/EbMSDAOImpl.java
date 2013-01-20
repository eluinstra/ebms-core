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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.activation.DataSource;

import nl.clockwork.common.dao.DAOException;
import nl.clockwork.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSSendEvent;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.xmldsig.ObjectFactory;
import nl.clockwork.ebms.model.xml.xmldsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
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
		private String serviceType;
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

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, String messageId, String refToMessageId, String fromRole, String toRole, String serviceType, String service, String action, String messageHeader, String content)
		{
			this(timestamp,cpaId,conversationId,null,messageId,refToMessageId,fromRole,toRole,serviceType,service,action,null,null,messageHeader,null,null,null,content,null);
		}

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, String messageId, String refToMessageId, String fromRole, String toRole, String serviceType, String service, String action, String messageHeader, String content, EbMSMessageStatus status)
		{
			this(timestamp,cpaId,conversationId,null,messageId,refToMessageId,fromRole,toRole,serviceType,service,action,null,null,messageHeader,null,null,null,content,status);
		}

		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, Long sequenceNr, String messageId, String refToMessageId, String fromRole, String toRole, String serviceType, String service, String action, String messageHeader, String syncReply, String messageOrder, String ackRequested, String content)
		{
			this(timestamp,cpaId,conversationId,sequenceNr,messageId,refToMessageId,fromRole,toRole,serviceType,service,action,null,null,messageHeader,syncReply,messageOrder,ackRequested,content,null);
		}
		
		public EbMSMessagePreparedStatement(Date timestamp, String cpaId, String conversationId, Long sequenceNr, String messageId, String refToMessageId, String fromRole, String toRole, String serviceType, String service, String action, byte[] original, String signature, String messageHeader, String syncReply, String messageOrder, String ackRequested, String content, EbMSMessageStatus status)
		{
			this.timestamp = timestamp;
			this.cpaId = cpaId;
			this.conversationId = conversationId;
			this.sequenceNr = sequenceNr;
			this.messageId = messageId;
			this.refToMessageId = refToMessageId;
			this.fromRole = fromRole;
			this.toRole = toRole;
			this.serviceType = serviceType;
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
					"service_type," +
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
				") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")" +
				"returning id"
			);
			//ps.setDate(1,new java.sql.Date(timestamp.getTime()));
			//ps.setString(1,String.format(getDateFormat(),timestamp));
			//ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
			ps.setObject(1,timestamp,Types.TIMESTAMP);
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
			ps.setString(9,serviceType);
			ps.setString(10,service);
			ps.setString(11,action);
			ps.setBytes(12,original);
			ps.setString(13,signature);
			ps.setString(14,messageHeader);
			ps.setString(15,syncReply);
			ps.setString(16,messageOrder);
			ps.setString(17,ackRequested);
			ps.setString(18,content);
			if (status == null)
				ps.setNull(19,java.sql.Types.INTEGER);
			else
				ps.setInt(19,status.id());
			//ps.setString(20,status == null ? null : String.format(getDateFormat(),timestamp));
			//ps.setTimestamp(20,status == null ? null : new Timestamp(timestamp.getTime()));
			//ps.setOject(20,status == null ? null : timestamp,Types.TIMESTAMP);
			return ps;
		}
	}

	public class IdExtractor implements ResultSetExtractor
	{

		@Override
		public Object extractData(ResultSet rs) throws SQLException, DataAccessException
		{
			if (rs.next())
				return rs.getLong("id");
			else
				return null;
		}
		
	}

	public EbMSDAOImpl(PlatformTransactionManager transactionManager, javax.sql.DataSource dataSource)
	{
		super(transactionManager,dataSource);
	}

	public EbMSDAOImpl(TransactionTemplate transactionTemplate, javax.sql.DataSource dataSource)
	{
		super(transactionTemplate,dataSource);
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
	public String getReceivedMessageIdsQuery(String messageContextFilter, int maxNr)
	{
		return "select message_id" +
		" from ebms_message" +
		" where status=" + EbMSMessageStatus.RECEIVED.id() +
		messageContextFilter +
		" order by time_stamp asc" +
		" limit " + maxNr;
	}

	@Override
	public void insertMessage(final EbMSMessage message, final List<EbMSSendEvent> sendEvents) throws DAOException
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
							Date timestamp = new Date();
							Long key = (Long)jdbcTemplate.query(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getType(),
											message.getMessageHeader().getService().getValue(),
											message.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(message.getMessageHeader()),
											XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()),
											XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()),
											XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()),
											XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest())
									),
									new IdExtractor()
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
									key,
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}

							List<Object[]> events = new ArrayList<Object[]>();
							for (EbMSSendEvent sendEvent : sendEvents)
							{
								//events.add(new Object[]{key,String.format(getDateFormat(),sendEvent.getTime())});
								events.add(new Object[]{key,sendEvent.getTime()});
							}
							simpleJdbcTemplate.batchUpdate
							(
								"insert into ebms_send_event (" +
								"ebms_message_id," +
								"time" +
								") values (?,?)",
								events
							);
						}
						catch (Exception e)
						{
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
	public void insertMessage(final EbMSMessage message, final EbMSMessageStatus status) throws DAOException
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
							Date timestamp = new Date();
							Long key = (Long)jdbcTemplate.query(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getType(),
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
									new IdExtractor()
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
									key,
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
						}
						catch (Exception e)
						{
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
	public void insertMessage(final EbMSMessage message, final EbMSMessageStatus status, final EbMSMessageError messageError, final EbMSSendEvent sendEvent) throws DAOException
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
							Date timestamp = new Date();
							Long key = (Long)jdbcTemplate.query(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getType(),
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
									new IdExtractor()
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
									key,
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							key = (Long)jdbcTemplate.query(
									new EbMSMessagePreparedStatement(
											timestamp,
											messageError.getMessageHeader().getCPAId(),
											messageError.getMessageHeader().getConversationId(),
											messageError.getMessageHeader().getMessageData().getMessageId(),
											messageError.getMessageHeader().getMessageData().getRefToMessageId(),
											messageError.getMessageHeader().getFrom().getRole(),
											messageError.getMessageHeader().getTo().getRole(),
											messageError.getMessageHeader().getService().getType(),
											messageError.getMessageHeader().getService().getValue(),
											messageError.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageError.getMessageHeader()),
											XMLMessageBuilder.getInstance(ErrorList.class).handle(messageError.getErrorList())
									),
									new IdExtractor()
							);
					
							simpleJdbcTemplate.update
							(
								"insert into ebms_send_event (" +
								"ebms_message_id," +
								"time" +
								") values (?,?)",
								key,
								//String.format(getDateFormat(),sendEvent.getTime())
								sendEvent.getTime()
							);
						}
						catch (Exception e)
						{
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
	public void insertMessage(final EbMSMessage message, final EbMSMessageStatus status, final EbMSAcknowledgment acknowledgment, final EbMSSendEvent sendEvent) throws DAOException
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
							Date timestamp = new Date();
							Long key = (Long)jdbcTemplate.query(
									new EbMSMessagePreparedStatement(
											timestamp,
											message.getMessageHeader().getCPAId(),
											message.getMessageHeader().getConversationId(),
											message.getMessageOrder() == null ? null : message.getMessageOrder().getSequenceNumber().getValue().longValue(),
											message.getMessageHeader().getMessageData().getMessageId(),
											message.getMessageHeader().getMessageData().getRefToMessageId(),
											message.getMessageHeader().getFrom().getRole(),
											message.getMessageHeader().getTo().getRole(),
											message.getMessageHeader().getService().getType(),
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
									new IdExtractor()
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
									key,
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							if (acknowledgment != null)
							{
								key = (Long)jdbcTemplate.query(
										new EbMSMessagePreparedStatement(
												timestamp,
												acknowledgment.getMessageHeader().getCPAId(),
												acknowledgment.getMessageHeader().getConversationId(),
												acknowledgment.getMessageHeader().getMessageData().getMessageId(),
												acknowledgment.getMessageHeader().getMessageData().getRefToMessageId(),
												acknowledgment.getMessageHeader().getFrom().getRole(),
												acknowledgment.getMessageHeader().getTo().getRole(),
												acknowledgment.getMessageHeader().getService().getType(),
												acknowledgment.getMessageHeader().getService().getValue(),
												acknowledgment.getMessageHeader().getAction(),
												XMLMessageBuilder.getInstance(MessageHeader.class).handle(acknowledgment.getMessageHeader()),
												XMLMessageBuilder.getInstance(Acknowledgment.class).handle(acknowledgment.getAcknowledgment())
										),
										new IdExtractor()
								);
					
								if (sendEvent != null)
									simpleJdbcTemplate.update
									(
										"insert into ebms_send_event (" +
										"ebms_message_id," +
										"time" +
										") values (?,?)",
										key,
										//String.format(getDateFormat(),sendEvent.getTime())
										sendEvent.getTime()
									);
							}
						}
						catch (Exception e)
						{
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
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											messageError.getMessageHeader().getCPAId(),
											messageError.getMessageHeader().getConversationId(),
											messageError.getMessageHeader().getMessageData().getMessageId(),
											messageError.getMessageHeader().getMessageData().getRefToMessageId(),
											messageError.getMessageHeader().getFrom().getRole(),
											messageError.getMessageHeader().getTo().getRole(),
											messageError.getMessageHeader().getService().getType(),
											messageError.getMessageHeader().getService().getValue(),
											messageError.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageError.getMessageHeader()),
											XMLMessageBuilder.getInstance(ErrorList.class).handle(messageError.getErrorList()),
											status
									)
							);

							Long id = getEbMSMessageId(messageError.getMessageHeader().getMessageData().getRefToMessageId());
							if (id != null)
								simpleJdbcTemplate.update
								(
									"delete from ebms_send_event" +
									" where ebms_message_id=? and status=0",
									id
								);
						}
						catch (Exception e)
						{
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
				new TransactionCallbackWithoutResult()
				{
	
					@Override
					public void doInTransactionWithoutResult(TransactionStatus transactionStatus)
					{
						try
						{
							Date timestamp = new Date();
							jdbcTemplate.update(
									new EbMSMessagePreparedStatement(
											timestamp,
											acknowledgment.getMessageHeader().getCPAId(),
											acknowledgment.getMessageHeader().getConversationId(),
											acknowledgment.getMessageHeader().getMessageData().getMessageId(),
											acknowledgment.getMessageHeader().getMessageData().getRefToMessageId(),
											acknowledgment.getMessageHeader().getFrom().getRole(),
											acknowledgment.getMessageHeader().getTo().getRole(),
											acknowledgment.getMessageHeader().getService().getType(),
											acknowledgment.getMessageHeader().getService().getValue(),
											acknowledgment.getMessageHeader().getAction(),
											XMLMessageBuilder.getInstance(MessageHeader.class).handle(acknowledgment.getMessageHeader()),
											XMLMessageBuilder.getInstance(Acknowledgment.class).handle(acknowledgment.getAcknowledgment()),
											status
									)
							);

							Long id = getEbMSMessageId(acknowledgment.getMessageHeader().getMessageData().getRefToMessageId());
							if (id != null)
								simpleJdbcTemplate.update
								(
									"delete from ebms_send_event" +
									" where ebms_message_id=? and status=0",
									id
								);
						}
						catch (Exception e)
						{
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
	
}
