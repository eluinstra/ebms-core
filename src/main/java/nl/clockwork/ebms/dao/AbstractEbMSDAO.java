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

import javax.activation.DataHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.querydsl.core.types.Expression;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.querydsl.InstantType;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.util.DOMUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
abstract class AbstractEbMSDAO implements EbMSDAO
{
	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QEbmsMessage table = QEbmsMessage.ebmsMessage;
	Expression<?>[] ebMSMessageContextColumns = {table.cpaId,table.fromPartyId,table.fromRole,table.toPartyId,table.toRole,table.service,table.action,table.timeStamp,table.conversationId,table.messageId,table.refToMessageId,table.status};
	RowMapper<EbMSMessageContext> ebMSMessageContextRowMapper = (rs,rowNum) -> 
	{
		return EbMSMessageContext.builder()
				.cpaId(rs.getString("cpa_id"))
				.fromParty(new Party(rs.getString("from_party_id"),rs.getString("from_role")))
				.toParty(new Party(rs.getString("to_party_id"),rs.getString("to_role")))
				.service(rs.getString("service"))
				.action(rs.getString("action"))
				.timestamp(InstantType.toInstant(rs.getTimestamp("time_stamp")))
				.conversationId(rs.getString("conversation_id"))
				.messageId(rs.getString("message_id"))
				.refToMessageId(rs.getString("ref_to_message_id"))
				.messageStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")).orElse(null))
				.build();
	};
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
	RowMapper<EbMSDataSource> ebMSDataSourceRowMapper =	(rs,rowNum) ->
	{
		try
		{
			return new EbMSDataSource(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),IOUtils.toByteArray(rs.getBinaryStream("content")));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	};
	RowMapper<EbMSDataSourceMTOM> ebMSDataSourceMTOMRowMapper =	(rs,rowNum) ->
	{
		try
		{
			val a = EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
			return new EbMSDataSourceMTOM(a.getContentId(),new DataHandler(a));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	};

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void executeTransaction(final Action action)
	{
		action.run();
	}

	@Override
	public boolean existsMessage(String messageId)
	{
		val query = queryFactory.select(table.messageId.count())
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.getSQL();
		return jdbcTemplate.queryForObject(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				Integer.class) > 0;
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message)
	{
		val query = queryFactory.select(table.messageId.count())
				.from(table)
				.where(table.messageId.eq(message.getMessageHeader().getMessageData().getMessageId())
						.and(table.messageNr.eq(0))
						.and(table.cpaId.eq(message.getMessageHeader().getCPAId()))
						/*.and(table.fromRole.eq(message.getMessageHeader().getFrom().getRole()))
						.and(table.toRole.eq(message.getMessageHeader().getTo().getRole()))
						.and(table.service.eq(message.getMessageHeader().getService()))
						.and(table.action.eq(message.getMessageHeader().getAction()))*/)
				.getSQL();
		return jdbcTemplate.queryForObject(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
			Integer.class) > 0;
	}

	@Override
	public Optional<EbMSMessageContent> getMessageContent(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceRowMapper);
		return getMessageContext(messageId).map(mc -> new EbMSMessageContent(mc,dataSources));
	}

	@Override
	public Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceMTOMRowMapper);
		return getMessageContext(messageId).map(mc -> new EbMSMessageContentMTOM(mc,dataSources));
	}

	@Override
	public Optional<EbMSMessageContext> getMessageContext(String messageId)
	{
		try
		{
			val query = queryFactory.select(ebMSMessageContextColumns)
					.from(table)
					.where(table.messageId.eq(messageId)
							.and(table.messageNr.eq(0)))
					.getSQL();
			return Optional.of(jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					ebMSMessageContextRowMapper));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		try
		{
			var whereClause = table.cpaId.eq(cpaId)
					.and(table.refToMessageId.eq(refToMessageId))
					.and(table.messageNr.eq(0));
			if (actions.length > 0)
				whereClause.and(table.service.eq(EbMSAction.EBMS_SERVICE_URI))
						.and(table.action.in(EbMSAction.getActions(actions)));
			val query = queryFactory.select(ebMSMessageContextColumns)
					.from(table)
					.where(whereClause)
					.getSQL();
			return Optional.of(jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					ebMSMessageContextRowMapper));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<Document> getDocument(String messageId)
	{
		try
		{
			val query = queryFactory.select(table.content)
					.from(table)
					.where(table.messageId.eq(messageId)
							.and(table.messageNr.eq(0)))
					.getSQL();
			return Optional.of(DOMUtils.read(
					jdbcTemplate.queryForObject(
							query.getSQL(),
							query.getNullFriendlyBindings().toArray(),
							String.class)));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (ParserConfigurationException | SAXException | IOException  e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId)
	{
		try
		{
			val query = queryFactory.select(table.content)
					.from(table)
					.where(table.messageId.eq(messageId)
							.and(table.messageNr.eq(0))
							.and(table.status.isNull().or(table.statusRaw.eq(EbMSMessageStatus.SENDING.getId()))))
					.getSQL();
			val content = jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					String.class);
			val builder = EbMSDocument.builder()
				.contentId(messageId)
				.message(DOMUtils.read(content))
				.attachments(getAttachments(messageId,ebMSAttachmentRowMapper));
			return Optional.of(builder.build());
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (ParserConfigurationException | SAXException | IOException  e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		try
		{
			var whereClause = table.cpaId.eq(cpaId)
					.and(table.refToMessageId.eq(refToMessageId))
					.and(table.messageNr.eq(0));
			if (actions.length > 0)
				whereClause.and(table.service.eq(EbMSAction.EBMS_SERVICE_URI))
						.and(table.action.in(EbMSAction.getActions(actions)));
			val query = queryFactory.select(table.messageId,table.content)
					.from(table)
					.where(whereClause)
					.getSQL();
			val document = jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					(rs,rowNum) ->
					{
						try
						{
							return EbMSDocument.builder()
									.contentId(rs.getString("message_id"))
									.message(DOMUtils.read(rs.getString("content")))
									.build();
						}
						catch (ParserConfigurationException | SAXException | IOException e)
						{
							throw new SQLException(e);
						}
					});
			val builder = EbMSDocument.builder()
					.contentId(document.getContentId())
					.message(document.getMessage())
					.attachments(getAttachments(refToMessageId,ebMSAttachmentRowMapper));
			return Optional.of(builder.build());
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}
	
	@Override
	public Optional<EbMSMessageStatus> getMessageStatus(String messageId)
	{
		try
		{
			val query = queryFactory.select(table.status)
					.from(table)
					.where(table.messageId.eq(messageId)
							.and(table.messageNr.eq(0)))
					.getSQL();
			return EbMSMessageStatus.get(jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					(rs,rowNum) ->
					{
						return rs.getObject("status",Integer.class);
					}));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<Instant> getPersistTime(String messageId)
	{
		val query = queryFactory.select(table.persistTime)
				.from(table)
				.where(table.messageId.eq(messageId)
						.and(table.messageNr.eq(0)))
				.getSQL();
		return InstantType.toOptionalInstant(jdbcTemplate.queryForObject(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				Timestamp.class));
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId)
	{
		try
		{
			val query = queryFactory.select(table.action)
					.from(table)
					.where(table.messageId.eq(messageId)
							.and(table.messageNr.eq(0)))
					.getSQL();
			return jdbcTemplate.queryForObject(
					query.getSQL(),
					query.getNullFriendlyBindings().toArray(),
					(rs,rowNum) ->
					{
						return EbMSAction.get(rs.getString("action"));
					});
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}
	
	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status)
	{
		var whereClause = table.messageNr.eq(0)
				.and(table.statusRaw.eq(status.getId()));
		whereClause = EbMSDAO.applyFilter(table,messageContext,whereClause);
		val query = queryFactory.select(table.status)
				.from(table)
				.where(whereClause)
				.orderBy(table.timeStamp.asc())
				.getSQL();
		return jdbcTemplate.queryForList(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				String.class);
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr)
	{
		var whereClause = table.messageNr.eq(0)
				.and(table.statusRaw.eq(status.getId()));
		whereClause = EbMSDAO.applyFilter(table,messageContext,whereClause);
		val query = queryFactory.select(table.status)
				.from(table)
				.where(whereClause)
				.orderBy(table.timeStamp.asc())
				.limit(maxNr)
				.getSQL();
		return jdbcTemplate.queryForList(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				String.class);
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void insertMessage(final Instant timestamp, final Instant persistTime, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments, final EbMSMessageStatus status)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try
						{
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
							val messageHeader = message.getMessageHeader();
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
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void insertDuplicateMessage(final Instant timestamp, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try
						{
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
							val messageHeader = message.getMessageHeader();
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
	public int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus)
	{
		return jdbcTemplate.update
		(
			"update ebms_message" +
			" set status = ?," +
			" status_time = ?" +
			" where message_id = ?" +
			" and message_nr = 0" +
			" and status = ?",
			newStatus.getId(),
			Timestamp.from(Instant.now()),
			messageId,
			oldStatus != null ? oldStatus.getId() : null
		);
	}

	@Override
	public void deleteAttachments(String messageId)
	{
		jdbcTemplate.update(
			"delete from ebms_attachment" +
			" where message_id = ?",
			messageId
		);
	}

	protected <T> List<T> getAttachments(String messageId, RowMapper<T> rowMapper)
	{
		return jdbcTemplate.query(
			"select name, content_id, content_type, content" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = 0" +
			" order by order_nr",
			rowMapper,
			messageId
		);
	}
}
