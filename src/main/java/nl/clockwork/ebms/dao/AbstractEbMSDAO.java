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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.w3c.dom.Document;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.CachedEbMSAttachment;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.querydsl.model.QEbmsAttachment;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.service.model.DataSource;
import nl.clockwork.ebms.service.model.MTOMDataSource;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageProperties;
import nl.clockwork.ebms.util.DOMUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class AbstractEbMSDAO implements EbMSDAO
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QEbmsMessage table = QEbmsMessage.ebmsMessage;
	QEbmsAttachment attachmentTable = QEbmsAttachment.ebmsAttachment;
	Expression<?>[] ebMSMessagePropertiesColumns = {table.cpaId,table.fromPartyId,table.fromRole,table.toPartyId,table.toRole,table.service,table.action,table.timeStamp,table.conversationId,table.messageId,table.refToMessageId,table.status};
	ConstructorExpression<EbMSMessageProperties> ebMSMessagePropertiesProjection =
			Projections.constructor(EbMSMessageProperties.class,ebMSMessagePropertiesColumns);
	Expression<?>[] messagePropertiesColumns = {table.cpaId,table.fromPartyId,table.fromRole,table.toPartyId,table.toRole,table.service,table.action,table.timeStamp,table.conversationId,table.messageId,table.refToMessageId,table.status};
	ConstructorExpression<MessageProperties> messagePropertiesProjection =
			Projections.constructor(MessageProperties.class,messagePropertiesColumns);
	Expression<?>[] attachmentColumns = {attachmentTable.name,attachmentTable.contentId,attachmentTable.contentType,attachmentTable.content};
	ConstructorExpression<CachedEbMSAttachment> ebMSAttachmentProjection = 
			Projections.constructor(CachedEbMSAttachment.class,attachmentTable.name,attachmentTable.contentId,attachmentTable.contentType,attachmentTable.content);
	RowMapper<EbMSAttachment> ebMSAttachmentRowMapper =	(rs,rowNum) ->
	{
		try
		{
			return EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	};
	RowMapper<DataSource> ebMSDataSourceRowMapper =	(rs,rowNum) ->
	{
		try
		{
			return new DataSource(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),IOUtils.toByteArray(rs.getBinaryStream("content")));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	};
	RowMapper<MTOMDataSource> ebMSDataSourceMTOMRowMapper =	(rs,rowNum) ->
	{
		try
		{
			val a = EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
			return new MTOMDataSource(a.getContentId(),new DataHandler(a));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	};

	@Override
	public boolean existsMessage(String messageId)
	{
		return queryFactory.select(table.messageId.count())
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne() > 0;
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message)
	{
		return queryFactory.select(table.messageId.count())
				.from(table)
				.where(table.messageId.eq(message.getMessageHeader().getMessageData().getMessageId())
						.and(table.messageNr.eq(0))
						.and(table.cpaId.eq(message.getMessageHeader().getCPAId()))
						/*.and(table.fromRole.eq(message.getMessageHeader().getFrom().getRole()))
						.and(table.toRole.eq(message.getMessageHeader().getTo().getRole()))
						.and(table.service.eq(message.getMessageHeader().getService()))
						.and(table.action.eq(message.getMessageHeader().getAction()))*/)
				.fetchOne() > 0;
	}

	@Override
	public Optional<Message> getMessage(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceRowMapper);
		return getMessageProperties(messageId).map(mc -> new Message(mc,dataSources));
	}

	@Override
	public Optional<MTOMMessage> getMTOMMessage(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceMTOMRowMapper);
		return getMessageProperties(messageId).map(mc -> new MTOMMessage(mc,dataSources));
	}

	protected Optional<MessageProperties> getMessageProperties(String messageId)
	{
		return Optional.ofNullable(queryFactory.select(messagePropertiesProjection)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}

	@Override
	public Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId)
	{
		return Optional.ofNullable(queryFactory.select(ebMSMessagePropertiesProjection)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}

	@Override
	public Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		var whereClause = table.cpaId.eq(cpaId)
				.and(table.refToMessageId.eq(refToMessageId))
				.and(table.messageNr.eq(0));
		if (actions.length > 0)
			whereClause.and(table.service.eq(EbMSAction.EBMS_SERVICE_URI))
					.and(table.action.in(EbMSAction.getActions(actions)));
		return Optional.ofNullable(queryFactory.select(ebMSMessagePropertiesProjection)
				.from(table)
				.where(whereClause)
				.fetchOne());
	}
	
	@Override
	public Optional<Document> getDocument(String messageId)
	{
		return Optional.ofNullable(queryFactory.select(table.content)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId)
	{
		val content = queryFactory.select(table.content)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0))
						.and(table.status.isNull().or(table.status.eq(EbMSMessageStatus.CREATED))))
				.fetchOne();
		if (content != null)
		{
			val builder = EbMSDocument.builder()
				.contentId(messageId)
				.message(content)
				//.attachments(getAttachments(messageId,ebMSAttachmentProjection).stream().map(a -> a).collect(Collectors.toList()));
				.attachments(getAttachments(messageId,ebMSAttachmentRowMapper).stream().map(a -> a).collect(Collectors.toList()));
			return Optional.of(builder.build());
		}
		else
			return Optional.empty();
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		var whereClause = table.cpaId.eq(cpaId)
				.and(table.refToMessageId.eq(refToMessageId))
				.and(table.messageNr.eq(0));
		if (actions.length > 0)
			whereClause.and(table.service.eq(EbMSAction.EBMS_SERVICE_URI))
					.and(table.action.in(EbMSAction.getActions(actions)));
		val result = queryFactory.select(table.messageId,table.content)
				.from(table)
				.where(whereClause)
				.fetchOne();
		if (result != null)
		{
			val document = EbMSDocument.builder()
					.contentId(result.get(table.messageId))
					.message(result.get(table.content))
					.build();
			val builder = EbMSDocument.builder()
					.contentId(document.getContentId())
					.message(document.getMessage())
					//.attachments(getAttachments(refToMessageId,ebMSAttachmentProjection).stream().map(a -> a).collect(Collectors.toList()));
					.attachments(getAttachments(refToMessageId,ebMSAttachmentRowMapper).stream().map(a -> a).collect(Collectors.toList()));
			return Optional.of(builder.build());
		}
		else
			return Optional.empty();
	}
	
	@Override
	public Optional<EbMSMessageStatus> getMessageStatus(String messageId)
	{
		return Optional.ofNullable(queryFactory.select(table.status)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}

	@Override
	public Optional<Instant> getPersistTime(String messageId)
	{
		return Optional.ofNullable(queryFactory.select(table.persistTime)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId)
	{
		return EbMSAction.get(queryFactory.select(table.action)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.fetchOne());
	}
	
	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status)
	{
		var whereClause = new BooleanBuilder(table.messageNr.eq(0)
				.and(table.status.eq(status))); 
		whereClause = EbMSDAO.applyFilter(table,messageFilter,whereClause);
		return queryFactory.select(table.messageId)
				.from(table)
				.where(whereClause)
				.orderBy(table.timeStamp.asc())
				.fetch();
	}

	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status, int maxNr)
	{
		var whereClause = new BooleanBuilder(table.messageNr.eq(0)
				.and(table.status.eq(status)));
		whereClause = EbMSDAO.applyFilter(table,messageFilter,whereClause);
		return queryFactory.select(table.messageId)
				.from(table)
				.where(whereClause)
				.orderBy(table.timeStamp.asc())
				.limit(maxNr)
				.fetch();
	}

	@Override
	public long insertMessage(final Instant timestamp, final Instant persistTime, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments, final EbMSMessageStatus status)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			int rowsAffected = jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try
						{
							val messageHeader = message.getMessageHeader();
							val ps = connection.prepareStatement
							(
								"insert into ebms_message (" +
									"time_stamp," +
									"cpa_id," +
									"conversation_id," +
									"message_id," +
									"ref_to_message_id," +
									"time_to_live," +
									"from_party_id," +
									"from_role," +
									"to_party_id," +
									"to_role," +
									"service," +
									"action," +
									"content," +
									"status," +
									"status_time," +
									"persist_time" +
								") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
								new int[]{4,5}
							);
							ps.setTimestamp(1,Timestamp.from(timestamp));
							ps.setString(2,messageHeader.getCPAId());
							ps.setString(3,messageHeader.getConversationId());
							ps.setString(4,messageHeader.getMessageData().getMessageId());
							ps.setString(5,messageHeader.getMessageData().getRefToMessageId());
							ps.setTimestamp(6,messageHeader.getMessageData().getTimeToLive() == null ? null : Timestamp.from(messageHeader.getMessageData().getTimeToLive()));
							ps.setString(7,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
							ps.setString(8,messageHeader.getFrom().getRole());
							ps.setString(9,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
							ps.setString(10,messageHeader.getTo().getRole());
							ps.setString(11,EbMSMessageUtils.toString(messageHeader.getService()));
							ps.setString(12,messageHeader.getAction());
							ps.setString(13,DOMUtils.toString(document,"UTF-8"));
							ps.setObject(14,status != null ? status.getId() : null,java.sql.Types.INTEGER);
							ps.setTimestamp(15,status != null ? Timestamp.from(timestamp) : null);
							ps.setTimestamp(16,persistTime != null ? Timestamp.from(persistTime) : null);
							return ps;
						}
						catch (TransformerException e)
						{
							throw new SQLException(e);
						}
					}
				},
				keyHolder
			);
			insertAttachments(keyHolder,attachments);
			return rowsAffected;
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	@Override
	public long insertDuplicateMessage(final Instant timestamp, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			int rowsAffected = jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try
						{
							val messageHeader = message.getMessageHeader();
							val ps = connection.prepareStatement
							(
								"insert into ebms_message (" +
									"time_stamp," +
									"cpa_id," +
									"conversation_id," +
									"message_id," +
									"message_nr," +
									"ref_to_message_id," +
									"time_to_live," +
									"from_party_id," +
									"from_role," +
									"to_party_id," +
									"to_role," +
									"service," +
									"action," +
									"content" +
								") values (?,?,?,?,(select max(message_nr) + 1 from ebms_message where message_id = ?),?,?,?,?,?,?,?,?,?)",
								new int[]{4,5}
							);
							ps.setTimestamp(1,Timestamp.from(timestamp));
							ps.setString(2,messageHeader.getCPAId());
							ps.setString(3,messageHeader.getConversationId());
							ps.setString(4,messageHeader.getMessageData().getMessageId());
							ps.setString(5,messageHeader.getMessageData().getMessageId());
							ps.setString(6,messageHeader.getMessageData().getRefToMessageId());
							ps.setTimestamp(7,messageHeader.getMessageData().getTimeToLive() == null ? null : Timestamp.from(messageHeader.getMessageData().getTimeToLive()));
							ps.setString(8,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
							ps.setString(9,messageHeader.getFrom().getRole());
							ps.setString(10,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
							ps.setString(11,messageHeader.getTo().getRole());
							ps.setString(12,EbMSMessageUtils.toString(messageHeader.getService()));
							ps.setString(13,messageHeader.getAction());
							ps.setString(14,DOMUtils.toString(document,"UTF-8"));
							return ps;
						}
						catch (TransformerException e)
						{
							throw new SQLException(e);
						}
					}
				},
				keyHolder
			);
			insertAttachments(keyHolder,attachments);
			return rowsAffected;
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	protected void insertAttachments(KeyHolder keyHolder, List<EbMSAttachment> attachments) throws InvalidDataAccessApiUsageException, IOException
	{
		val orderNr = new AtomicInteger();
		for (val attachment: attachments)
		{
			jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try (val a = attachment)
						{
							val ps = connection.prepareStatement
							(
								"insert into ebms_attachment (" +
								"message_id," +
								"message_nr," +
								"order_nr," +
								"name," +
								"content_id," +
								"content_type," +
								"content" +
								") values (?,?,?,?,?,?,?)"
							);
							ps.setObject(1,keyHolder.getKeys().get("message_id"));
							ps.setObject(2,keyHolder.getKeys().get("message_nr"));
							ps.setInt(3,orderNr.getAndIncrement());
							ps.setString(4,a.getName());
							ps.setString(5,a.getContentId());
							ps.setString(6,a.getContentType());
							ps.setBinaryStream(7,a.getInputStream());
							return ps;
						}
						catch (IOException e)
						{
							throw new SQLException(e);
						}
					}
				}
			);
		}
	}

	@Override
	public long updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus)
	{
		return queryFactory.update(table)
				.set(table.status,newStatus)
				.set(table.statusTime,Instant.now())
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0))
						.and(oldStatus == null ? table.status.isNull() : table.status.eq(oldStatus)))
				.execute();
	}

	@Override
	public long deleteAttachments(String messageId)
	{
		return queryFactory.delete(attachmentTable)
				.where(attachmentTable.messageId.eq(messageId))
				.execute();
	}

	protected <T> List<T> getAttachments(String messageId, ConstructorExpression<T> projection)
	{
		return queryFactory.select(projection)
				.from(attachmentTable)
				.where(attachmentTable.messageId.eq(messageId)
						.and(attachmentTable.messageNr.eq(0)))
				.orderBy(attachmentTable.orderNr.asc())
				.fetch();
	}

	protected <T> List<T> getAttachments(String messageId, RowMapper<T> rowMapper)
	{
		val query = queryFactory.select(attachmentTable.name,attachmentTable.contentId,attachmentTable.contentType,attachmentTable.content)
				.from(attachmentTable)
				.where(attachmentTable.messageId.eq(messageId)
						.and(attachmentTable.messageNr.eq(0)))
				.orderBy(attachmentTable.orderNr.asc())
				.getSQL();
		return jdbcTemplate.query(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				rowMapper);
	}
}
