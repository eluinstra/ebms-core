/*
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import io.vavr.Tuple;
import io.vavr.Tuple2;
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
import nl.clockwork.ebms.model.EbMSMessageProperties;
import nl.clockwork.ebms.model.Party;
import nl.clockwork.ebms.service.model.DataSource;
import nl.clockwork.ebms.service.model.MTOMDataSource;
import nl.clockwork.ebms.service.model.MTOMMessage;
import nl.clockwork.ebms.service.model.Message;
import nl.clockwork.ebms.service.model.MessageFilter;
import nl.clockwork.ebms.service.model.MessageProperties;
import nl.clockwork.ebms.util.DOMUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
abstract class AbstractEbMSDAO implements EbMSDAO
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSMessageContextRowMapper implements RowMapper<EbMSMessageProperties>
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
		public EbMSMessageProperties mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return EbMSMessageProperties.builder()
					.cpaId(rs.getString("cpa_id"))
					.fromParty(Party.of(rs.getString("from_party_id"),rs.getString("from_role")))
					.toParty(Party.of(rs.getString("to_party_id"),rs.getString("to_role")))
					.service(rs.getString("service"))
					.action(rs.getString("action"))
					.timestamp(rs.getTimestamp("time_stamp").toInstant())
					.conversationId(rs.getString("conversation_id"))
					.messageId(rs.getString("message_id"))
					.refToMessageId(rs.getString("ref_to_message_id"))
					.messageStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")).orElse(null))
					.build();
		}
	}

	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class MessageContextRowMapper implements RowMapper<MessageProperties>
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
		public MessageProperties mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return MessageProperties.builder()
					.cpaId(rs.getString("cpa_id"))
					.fromParty(new nl.clockwork.ebms.service.model.Party(rs.getString("from_party_id"),rs.getString("from_role")))
					.toParty(new nl.clockwork.ebms.service.model.Party(rs.getString("to_party_id"),rs.getString("to_role")))
					.service(rs.getString("service"))
					.action(rs.getString("action"))
					.timestamp(rs.getTimestamp("time_stamp").toInstant())
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
	@NonNull
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
	public void executeTransaction(final Runnable runnable)
	{
		transactionTemplate.executeWithoutResult(
			new Consumer<TransactionStatus>()
			{
				@Override
				public void accept(TransactionStatus t)
				{
					runnable.run();
				}
			}
		);
	}

	@Override
	public boolean existsMessage(String messageId)
	{
		return jdbcTemplate.queryForObject("select count(message_id) from ebms_message where message_id = ? and message_nr = 0",Integer.class,messageId) > 0;
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
	public Optional<Message> getMessage(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceRowMapper);
		return getMessageProperties(messageId).map(p -> new Message(p,dataSources));
	}

	@Override
	public Optional<MTOMMessage> getMTOMMessage(String messageId)
	{
		val dataSources = getAttachments(messageId,ebMSDataSourceMTOMRowMapper);
		return getMessageProperties(messageId).map(p -> new MTOMMessage(p,dataSources));
	}

	protected Optional<MessageProperties> getMessageProperties(String messageId)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				MessageContextRowMapper.SELECT + " from ebms_message where message_id = ? and message_nr = 0",
				new MessageContextRowMapper(),
				messageId
			));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				EbMSMessageContextRowMapper.SELECT + " from ebms_message where message_id = ? and message_nr = 0",
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
	public Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				EbMSMessageContextRowMapper.SELECT +
				" from ebms_message" + 
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(actions.length == 0 
						? "" 
						: " and service = '" + EbMSAction.EBMS_SERVICE_URI + "' and action in ('" + Arrays.stream(actions).map(a -> a.getAction()).collect(Collectors.joining("','")) + "')"),
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
		catch (ParserConfigurationException | SAXException | IOException e)
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
					" and (status is null or status = " + EbMSMessageStatus.CREATED.getId() + ")",
					String.class,
					messageId);
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
			val document = jdbcTemplate.queryForObject(
					"select message_id, content" +
					" from ebms_message" +
					" where cpa_id = ?" +
					" and ref_to_message_id = ?" +
					" and message_nr = 0" +
					(actions.length == 0 
							? "" 
							: " and service = '" + EbMSAction.EBMS_SERVICE_URI + "' and action in ('" + Arrays.stream(actions).map(a -> a.getAction()).collect(Collectors.joining("','")) + "')"),
					new RowMapper<EbMSDocument>()
					{
						@Override
						public EbMSDocument mapRow(ResultSet rs, int rowNum) throws SQLException
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
						}
					},
					cpaId,
					refToMessageId
				);
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
	public Optional<Instant> getPersistTime(String messageId)
	{
		return Optional.ofNullable(jdbcTemplate.queryForObject("select persist_time from ebms_message where message_id = ? and message_nr = 0",Timestamp.class,messageId))
				.map(r -> r.toInstant());
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId)
	{
		try
		{
			return EbMSAction.get(jdbcTemplate.queryForObject("select action from ebms_message where message_id = ? and message_nr = 0",String.class,messageId));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}
	
	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status)
	{
		val parameters = new ArrayList<Object>();
		return jdbcTemplate.queryForList(
				"select message_id" +
				" from ebms_message" +
				" where message_nr = 0" +
				" and status = " + status.getId() +
				EbMSDAO.getMessageFilter(messageFilter,parameters) +
				" order by time_stamp asc",
				parameters.toArray(new Object[0]),
				String.class);
	}

	public abstract String getMessageIdsQuery(String messageContextFilter, EbMSMessageStatus status, int maxNr);

	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status, int maxNr)
	{
		val parameters = new ArrayList<Object>();
		val messageContextFilter = EbMSDAO.getMessageFilter(messageFilter,parameters);
		return jdbcTemplate.queryForList(
				getMessageIdsQuery(messageContextFilter,status,maxNr),
				parameters.toArray(new Object[0]),
				String.class);
	}

	@Override
	public String insertMessage(final Instant timestamp, final Instant persistTime, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments, final EbMSMessageStatus status)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(c ->
			{
				try
				{
					val messageHeader = message.getMessageHeader();
					val ps = c.prepareStatement
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
			},
			keyHolder);
			insertAttachments(keyHolder,attachments);
			return (String)keyHolder.getKeys().get("message_id");
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("",e);
		}
	}

	@Override
	public Tuple2<String,Integer> insertDuplicateMessage(final Instant timestamp, final Document document, final EbMSBaseMessage message, final List<EbMSAttachment> attachments)
	{
		try
		{
			val keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(c ->
			{
				try
				{
					val messageHeader = message.getMessageHeader();
					val ps = c.prepareStatement
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
			},
			keyHolder);
			insertAttachments(keyHolder,attachments);
			return Tuple.of((String)keyHolder.getKeys().get("message_id"),(Integer)keyHolder.getKeys().get("message_nr"));
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
			jdbcTemplate.update(c ->
			{
				try (val a = attachment)
				{
					val ps = c.prepareStatement("insert into ebms_attachment (message_id,message_nr,order_nr,name,content_id,content_type,content) values (?,?,?,?,?,?,?)");
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
			});
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
	public int deleteAttachments(String messageId)
	{
		return jdbcTemplate.update(
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
