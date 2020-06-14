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

import static nl.clockwork.ebms.dao.EbMSMessageMapper.EbMSMessageDSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSAction;
import nl.clockwork.ebms.EbMSAttachmentFactory;
import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.transaction.TransactionCallback;
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
	EbMSMessageMapper mapper;
	
	@Override
	public void executeTransaction(final TransactionCallback callback) throws DAOException
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

	@Override
	public boolean existsMessage(String messageId) throws DAOException
	{
		val s = select(count())
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.count(s) > 0;
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message) throws DAOException
	{
		val s = select(count())
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(message.getMessageHeader().getMessageData().getMessageId()))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.and(ebMSMessageTable.cpaId,isEqualTo(message.getMessageHeader().getCPAId()))
//				.and(ebMSMessageTable.fromRole,isEqualTo(message.getMessageHeader().getFrom().getRole()))
//				.and(ebMSMessageTable.toRole,isEqualTo(message.getMessageHeader().getTo().getRole()))
//				.and(ebMSMessageTable.service,isEqualTo(message.getMessageHeader().getService()))
//				.and(ebMSMessageTable.action,isEqualTo(message.getMessageHeader().getAction()))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.count(s) > 0;
	}

	@Override
	public Optional<EbMSMessageContent> getMessageContent(String messageId) throws DAOException
	{
		try
		{
			val dataSources = new ArrayList<EbMSDataSource>();
			val attachments = getAttachments(messageId);
//			val dataSources = attachments.stream()
//					.map(a -> EbMSDataSource.builder()
//						.name(a.getName())
//						.contentId(a.getContentId())
//						.contentType(a.getContentType())
//						.content(IOUtils.toByteArray(a.getInputStream()))
//						.build())
//					.collect(Collectors.toList());
			for (val attachment: attachments)
				dataSources.add(EbMSDataSource.builder()
						.name(attachment.getName())
						.contentId(attachment.getContentId())
						.contentType(attachment.getContentType())
						.content(IOUtils.toByteArray(attachment.getInputStream()))
						.build());
			return getMessageContext(messageId).map(mc -> new EbMSMessageContent(mc,dataSources)
			);
		}
		catch (DataAccessException | IOException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId) throws DAOException
	{
		try
		{
			val attachments = getAttachments(messageId);
			val dataSources = attachments.stream()
					.map(a -> new EbMSDataSourceMTOM(a.getContentId(),new DataHandler(a)))
					.collect(Collectors.toList());
			return getMessageContext(messageId).map(mc -> new EbMSMessageContentMTOM(mc,dataSources)
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<EbMSMessageContext> getMessageContext(String messageId) throws DAOException
	{
		val s = select(ebMSMessageTable.messageContext)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageContext(s);
	}

	@Override
	public Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions) throws DAOException
	{
		val b = select(ebMSMessageTable.messageContext)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.cpaId,isEqualTo(cpaId))
				.and(ebMSMessageTable.refToMessageId,isEqualTo(refToMessageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0));
		if (actions.length != 0)
		{
			b.and(ebMSMessageTable.service,isEqualTo(EbMSAction.EBMS_SERVICE_URI));
			b.and(ebMSMessageTable.action,isIn(EbMSAction.toStringList(actions)));
		}
		val s = b.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageContext(s);
	}
	
	@Override
	public Optional<Document> getDocument(String messageId) throws DAOException
	{
		try
		{
			val s = select(ebMSMessageTable.content)
					.from(ebMSMessageTable)
					.where(ebMSMessageTable.messageId,isEqualTo(messageId))
					.and(ebMSMessageTable.messageNr,isEqualTo(0))
					.build()
					.render(RenderingStrategies.MYBATIS3);
			val result = mapper.selectContent(s);
			return Optional.ofNullable(result.isPresent() ? DOMUtils.read(result.get()) : null);
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId) throws DAOException
	{
//		val s = select(ebMSMessageTable.content)
//				.from(ebMSMessageTable)
//				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
//				.and(ebMSMessageTable.messageNr,isEqualTo(0))
//				.and(ebMSMessageTable.status,or(ebMSMessageTable.status,isEqualTo(EbMSMessageStatus.SENDING.getId())))
//				.build()
//				.render(RenderingStrategies.MYBATIS3);
//		return mapper.selectEbMSDocumentIfUnsent(messageId,EbMSMessageStatus.SENDING.getId());
		try
		{
			val content = jdbcTemplate.queryForObject(
					"select content" +
					" from ebms_message" +
					" where message_id = ?" +
					" and message_nr = 0" +
					" and (status is null or status = " + EbMSMessageStatus.SENDING.getId() + ")",
					String.class,
					messageId);
			val builder = EbMSDocument.builder()
				.contentId(messageId)
				.message(DOMUtils.read(content))
				.attachments(getAttachments(messageId));
			return Optional.of(builder.build());
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (DataAccessException | ParserConfigurationException | SAXException | IOException  e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions) throws DAOException
	{
		try
		{
			val document = jdbcTemplate.queryForObject(
				"select message_id, content" +
				" from ebms_message" +
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(actions.length == 0 ? "" : " and service = '" + EbMSAction.EBMS_SERVICE_URI + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')"),
				(RowMapper<EbMSDocument>)(rs,rowNum) ->
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
				},
				cpaId,
				refToMessageId
			);
			val builder = EbMSDocument.builder()
					.contentId(document.getContentId())
					.message(document.getMessage())
					.attachments(getAttachments(refToMessageId));
			return Optional.of(builder.build());
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Optional<EbMSMessageStatus> getMessageStatus(String messageId) throws DAOException
	{
		val s = select(ebMSMessageTable.status)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageStatus(s);
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId) throws DAOException
	{
		val s = select(ebMSMessageTable.action)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageAction(s).flatMap(a -> EbMSAction.get(a));
	}
	
	@Override
	public Optional<Instant> getPersistTime(String messageId)
	{
		val s = select(ebMSMessageTable.persistTime)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectPersistTime(s);
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException
	{
		val b = select(ebMSMessageTable.messageId)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageNr,isEqualTo(0))
				.and(ebMSMessageTable.status,isEqualTo(status));
		applyMessageContextFilter(b,messageContext);
		val s = b.orderBy(ebMSMessageTable.timestamp)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageIds(s);
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr) throws DAOException
	{
		val b = select(ebMSMessageTable.messageId)
				.from(ebMSMessageTable)
				.where(ebMSMessageTable.messageNr,isEqualTo(0))
				.and(ebMSMessageTable.status,isEqualTo(status));
		applyMessageContextFilter(b,messageContext);
		val s = b.orderBy(ebMSMessageTable.timestamp)
				.limit(maxNr)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMessageIds(s);
	}

	public void applyMessageContextFilter(QueryExpressionDSL<org.mybatis.dynamic.sql.select.SelectModel>.QueryExpressionWhereBuilder s, EbMSMessageContext messageContext)
	{
		if (messageContext != null)
		{
			s.and(ebMSMessageTable.cpaId,isEqualTo(messageContext.getCpaId()).when(v -> v != null));
			if (messageContext.getFromParty() != null)
			{
				s.and(ebMSMessageTable.fromPartyId,isEqualTo(messageContext.getFromParty().getPartyId()).when(v -> v != null));
				s.and(ebMSMessageTable.fromRole,isEqualTo(messageContext.getFromParty().getPartyId()).when(v -> v != null));
			}
			if (messageContext.getToParty() != null)
			{
				s.and(ebMSMessageTable.toPartyId,isEqualTo(messageContext.getToParty().getPartyId()).when(v -> v != null));
				s.and(ebMSMessageTable.toRole,isEqualTo(messageContext.getToParty().getPartyId()).when(v -> v != null));
			}
			s.and(ebMSMessageTable.service,isEqualTo(messageContext.getService()).when(v -> v != null));
			s.and(ebMSMessageTable.action,isEqualTo(messageContext.getAction()).when(v -> v != null));
			s.and(ebMSMessageTable.conversationId,isEqualTo(messageContext.getConversationId()).when(v -> v != null));
			s.and(ebMSMessageTable.messageId,isEqualTo(messageContext.getMessageId()).when(v -> v != null));
			s.and(ebMSMessageTable.refToMessageId,isEqualTo(messageContext.getRefToMessageId()).when(v -> v != null));
			s.and(ebMSMessageTable.status,isEqualTo(messageContext.getMessageStatus()).when(v -> v != null));
		}
	}

	@Override
	public void insertMessage(final Instant timestamp, final Instant persistTime, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments, final EbMSMessageStatus status) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
					@Override
					public void doInTransactionWithoutResult(TransactionStatus arg0)
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
							throw new DAOException(e);
						}
					}
				}
			);
		}
		catch (DataAccessException | TransactionException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertDuplicateMessage(final Instant timestamp, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments) throws DAOException
	{
		try
		{
			transactionTemplate.execute(
				new TransactionCallbackWithoutResult()
				{
					@Override
					public void doInTransactionWithoutResult(TransactionStatus arg0)
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
							throw new DAOException(e);
						}
					}
				}
			);
		}
		catch (DataAccessException | TransactionException e)
		{
			throw new DAOException(e);
		}
	}

	protected void insertAttachments(KeyHolder keyHolder, List<EbMSAttachment> attachments) throws InvalidDataAccessApiUsageException, DataAccessException, IOException
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
	public int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus) throws DAOException
	{
		val s = update(ebMSMessageTable)
				.set(ebMSMessageTable.status).equalTo(newStatus)
				.set(ebMSMessageTable.statusTime).equalTo(Instant.now())
				.where(ebMSMessageTable.messageId,isEqualTo(messageId))
				.and(ebMSMessageTable.messageNr,isEqualTo(0))
				.and(ebMSMessageTable.status,isNull().when(() -> oldStatus == null))
				.and(ebMSMessageTable.status,isEqualTo(oldStatus).when(s -> s != null))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.update(s);
	}

	@Override
	public void deleteAttachments(String messageId)
	{
		val s = deleteFrom(ebMSAttachmentTable)
				.where(ebMSAttachmentTable.messageId,isEqualTo(messageId))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.delete(s);
	}

	protected List<EbMSAttachment> getAttachments(String messageId)
	{
		return jdbcTemplate.query(
			"select name, content_id, content_type, content" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = 0" +
			" order by order_nr",
			(RowMapper<EbMSAttachment>)(rs,rowNum) ->
			{
				try
				{
					return EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
				}
				catch (IOException e)
				{
					throw new EbMSProcessingException(e);
				}
			},
			messageId
		);
	}
}
