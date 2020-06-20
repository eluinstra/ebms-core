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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
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
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSMessageContextRowMapper implements RowMapper<EbMSMessageContext>
	{
		public static final String SELECT =
				"select cpa_id," +
				" from_party_id," +
				" from_role," +
				" to_party_id," +
				" to_role," +
				" service," +
				" action," +
				" time_stamp," +
				" conversation_id," +
				" message_id," +
				" ref_to_message_id," +
				" status";

		@Override
		public EbMSMessageContext mapRow(ResultSet rs, int rowNum) throws SQLException
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
		}
	}

	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	JdbcTemplate jdbcTemplate;
	
	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void executeTransaction(final Action action)
	{
		action.run();
	}

	@Override
	public boolean existsMessage(String messageId)
	{
		return jdbcTemplate.queryForObject(
			"select count(message_id)" +
			" from ebms_message" +
			" where message_id = ?" +
			" and message_nr = 0",
			Integer.class,
			messageId
		) > 0;
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message)
	{
		return jdbcTemplate.queryForObject(
			"select count(message_id)" +
			" from ebms_message" +
			" where message_id = ?" +
			" and message_nr = 0" +
			" and cpa_id = ?" /*+
			" and from_role =?" +
			" and to_role = ?" +
			" and service = ?" +
			" and action = ?"*/,
			Integer.class,
			message.getMessageHeader().getMessageData().getMessageId(),
			message.getMessageHeader().getCPAId()/*,
			message.getMessageHeader().getFrom().getRole(),
			message.getMessageHeader().getTo().getRole(),
			message.getMessageHeader().getService(),
			message.getMessageHeader().getAction()*/
		) > 0;
	}

	@Override
	public Optional<EbMSMessageContent> getMessageContent(String messageId)
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
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	@Override
	public Optional<EbMSMessageContentMTOM> getMessageContentMTOM(String messageId)
	{
		val attachments = getAttachments(messageId);
		val dataSources = attachments.stream()
				.map(a -> new EbMSDataSourceMTOM(a.getContentId(),new DataHandler(a)))
				.collect(Collectors.toList());
		return getMessageContext(messageId).map(mc -> new EbMSMessageContentMTOM(mc,dataSources)
		);
	}

	@Override
	public Optional<EbMSMessageContext> getMessageContext(String messageId)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				EbMSMessageContextRowMapper.SELECT +
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = 0",
				new EbMSMessageContextRowMapper(),
				messageId
			));
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
			return Optional.of(jdbcTemplate.queryForObject(
				EbMSMessageContextRowMapper.SELECT +
				" from ebms_message" + 
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(actions.length == 0 ? "" : " and service = '" + EbMSAction.EBMS_SERVICE_URI + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')"),
				new EbMSMessageContextRowMapper(),
				cpaId,
				refToMessageId
			));
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
			return Optional.of(DOMUtils.read(
					jdbcTemplate.queryForObject(
							"select content" +
							" from ebms_message" +
							" where message_id = ?" +
							" and message_nr = 0",
							String.class,
							messageId
						)
			));
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
			val document = jdbcTemplate.queryForObject(
				"select message_id, content" +
				" from ebms_message" +
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(actions.length == 0 ? "" : " and service = '" + EbMSAction.EBMS_SERVICE_URI + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')"),
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
	}
	
	@Override
	public Optional<EbMSMessageStatus> getMessageStatus(String messageId)
	{
		try
		{
			return EbMSMessageStatus.get(
				jdbcTemplate.queryForObject(
					"select status" +
					" from ebms_message" +
					" where message_id = ?" +
					" and message_nr = 0",
					(rs,rowNum) ->
					{
						return rs.getObject("status",Integer.class);
					},
					messageId
				)
			);
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<Instant> getPersistTime(String messageId)
	{
		return Optional.ofNullable(InstantType.toInstant(jdbcTemplate.queryForObject("select persist_time from ebms_message where message_id = ? and message_nr = 0",Timestamp.class,messageId)));
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId)
	{
		try
		{
			return jdbcTemplate.queryForObject(
					"select action" +
					" from ebms_message" +
					" where message_id = ?" +
					" and message_nr = 0",
					(rs,rowNum) ->
					{
						return EbMSAction.get(rs.getString("action"));
					},
					messageId
				);
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}
	
	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status)
	{
		val parameters = new ArrayList<Object>();
		return jdbcTemplate.queryForList(
				"select message_id" +
				" from ebms_message" +
				" where message_nr = 0" +
				" and status = " + status.getId() +
				getMessageContextFilter(messageContext,parameters) +
				" order by time_stamp asc",
				parameters.toArray(new Object[0]),
				String.class
		);
	}

	public abstract String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr);

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status, int maxNr)
	{
		val parameters = new ArrayList<Object>();
		val messageContextFilter = getMessageContextFilter(messageContext,parameters);
		return jdbcTemplate.queryForList(
				getMessageIdsQuery(messageContextFilter,status,maxNr),
				parameters.toArray(new Object[0]),
				String.class
		);
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

	protected List<EbMSAttachment> getAttachments(String messageId)
	{
		return jdbcTemplate.query(
			"select name, content_id, content_type, content" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = 0" +
			" order by order_nr",
			(rs,rowNum) ->
			{
				try
				{
					return EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
				}
				catch (IOException e)
				{
					throw new DataRetrievalFailureException("",e);
				}
			},
			messageId
		);
	}

	protected String getMessageContextFilter(EbMSMessageContext messageContext, List<Object> parameters)
	{
		val result = new StringBuffer();
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
			{
				parameters.add(messageContext.getCpaId());
				result.append(" and ebms_message.cpa_id = ?");
			}
			if (messageContext.getFromParty() != null)
			{
				if (messageContext.getFromParty().getPartyId() != null)
				{
					parameters.add(messageContext.getFromParty().getPartyId());
					result.append(" and ebms_message.from_party_id = ?");
				}
				if (messageContext.getFromParty().getRole() != null)
				{
					parameters.add(messageContext.getFromParty().getRole());
					result.append(" and ebms_message.from_role = ?");
				}
			}
			if (messageContext.getToParty() != null)
			{
				if (messageContext.getToParty().getPartyId() != null)
				{
					parameters.add(messageContext.getToParty().getPartyId());
					result.append(" and ebms_message.to_party_id = ?");
				}
				if (messageContext.getToParty().getRole() != null)
				{
					parameters.add(messageContext.getToParty().getRole());
					result.append(" and ebms_message.to_role = ?");
				}
			}
			if (messageContext.getService() != null)
			{
				parameters.add(messageContext.getService());
				result.append(" and ebms_message.service = ?");
			}
			if (messageContext.getAction() != null)
			{
				parameters.add(messageContext.getAction());
				result.append(" and ebms_message.action = ?");
			}
			if (messageContext.getConversationId() != null)
			{
				parameters.add(messageContext.getConversationId());
				result.append(" and ebms_message.conversation_id = ?");
			}
			if (messageContext.getMessageId() != null)
			{
				parameters.add(messageContext.getMessageId());
				result.append(" and ebms_message.message_id = ?");
			}
			if (messageContext.getRefToMessageId() != null)
			{
				parameters.add(messageContext.getRefToMessageId());
				result.append(" and ebms_message.ref_to_message_id = ?");
			}
			if (messageContext.getMessageStatus() != null)
			{
				parameters.add(messageContext.getMessageStatus().getId());
				result.append(" and ebms_message.status = ?");
			}
		}
		return result.toString();
	}
}
