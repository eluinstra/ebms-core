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


import jakarta.activation.DataHandler;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
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
import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
class EbMSDAOImpl implements EbMSDAO, WithMessageFilter
{
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSMessageContextRowMapper implements RowMapper<EbMSMessageProperties>
	{
		public static final String SELECT = "select cpa_id,"
				+ " from_party_id,"
				+ " from_role,"
				+ " to_party_id,"
				+ " to_role,"
				+ " service,"
				+ " action,"
				+ " time_stamp,"
				+ " conversation_id,"
				+ " message_id,"
				+ " ref_to_message_id,"
				+ " status";

		@Override
		public EbMSMessageProperties mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return EbMSMessageProperties.builder()
					.cpaId(rs.getString("cpa_id"))
					.fromParty(Party.of(rs.getString("from_party_id"), rs.getString("from_role")))
					.toParty(Party.of(rs.getString("to_party_id"), rs.getString("to_role")))
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
		public static final String SELECT = "select cpa_id,"
				+ " from_party_id,"
				+ " from_role,"
				+ " to_party_id,"
				+ " to_role,"
				+ " service,"
				+ " action,"
				+ " time_stamp,"
				+ " conversation_id,"
				+ " message_id,"
				+ " ref_to_message_id,"
				+ " status";

		@Override
		public MessageProperties mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return MessageProperties.builder()
					.cpaId(rs.getString("cpa_id"))
					.fromParty(new nl.clockwork.ebms.service.model.Party(rs.getString("from_party_id"), rs.getString("from_role")))
					.toParty(new nl.clockwork.ebms.service.model.Party(rs.getString("to_party_id"), rs.getString("to_role")))
					.service(rs.getString("service"))
					.action(rs.getString("action"))
					.timestamp(rs.getTimestamp("time_stamp").toInstant())
					.conversationId(rs.getString("conversation_id"))
					.messageId(rs.getString("message_id"))
					.refToMessageId(rs.getString("ref_to_message_id"))
					.messageStatus(EbMSMessageStatus.get(rs.getInt("status")).orElseThrow())
					.build();
		}
	}

	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	RowMapper<EbMSAttachment> ebMSAttachmentRowMapper = (rs, rowNum) ->
	{
		try
		{
			return EbMSAttachmentFactory
					.createCachedEbMSAttachment(rs.getString("name"), rs.getString("content_id"), rs.getString("content_type"), rs.getBinaryStream("content"));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("", e);
		}
	};
	RowMapper<DataSource> ebMSDataSourceRowMapper = (rs, rowNum) ->
	{
		try
		{
			return new DataSource(rs.getString("name"), rs.getString("content_id"), rs.getString("content_type"), IOUtils.toByteArray(rs.getBinaryStream("content")));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("", e);
		}
	};
	RowMapper<MTOMDataSource> ebMSDataSourceMTOMRowMapper = (rs, rowNum) ->
	{
		try
		{
			val a = EbMSAttachmentFactory
					.createCachedEbMSAttachment(rs.getString("name"), rs.getString("content_id"), rs.getString("content_type"), rs.getBinaryStream("content"));
			return new MTOMDataSource(a.getContentId(), new DataHandler(a));
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("", e);
		}
	};

	@Override
	public void executeTransaction(final Runnable runnable)
	{
		transactionTemplate.executeWithoutResult(t -> runnable.run());
	}

	@Override
	public boolean existsMessage(String messageId)
	{
		return jdbcTemplate.queryForObject("select count(message_id) from ebms_message where message_id = ?", Integer.class, messageId) > 0;
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message)
	{
		return jdbcTemplate.queryForObject(
				"select count(message_id)" + " from ebms_message" + " where message_id = ?" + " and cpa_id = ?" /*
																																																				 * + " and from_role =?" + " and to_role = ?" +
																																																				 * " and service = ?" + " and action = ?"
																																																				 */,
				Integer.class,
				message.getMessageHeader().getMessageData().getMessageId(),
				message.getMessageHeader().getCPAId()/*
																							 * , message.getMessageHeader().getFrom().getRole(), message.getMessageHeader().getTo().getRole(),
																							 * message.getMessageHeader().getService(), message.getMessageHeader().getAction()
																							 */
		) > 0;
	}

	@Override
	public Optional<Message> getMessage(String messageId)
	{
		val dataSources = getAttachments(messageId, ebMSDataSourceRowMapper);
		return getMessageProperties(messageId).map(p -> new Message(p, dataSources));
	}

	@Override
	public Optional<MTOMMessage> getMTOMMessage(String messageId)
	{
		val dataSources = getAttachments(messageId, ebMSDataSourceMTOMRowMapper);
		return getMessageProperties(messageId).map(p -> new MTOMMessage(p, dataSources));
	}

	protected Optional<MessageProperties> getMessageProperties(String messageId)
	{
		try
		{
			return Optional.of(
					jdbcTemplate.queryForObject(MessageContextRowMapper.SELECT + " from ebms_message where message_id = ?", new MessageContextRowMapper(), messageId));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<EbMSMessageProperties> getEbMSMessageProperties(String messageId)
	{
		try
		{
			return Optional.of(
					jdbcTemplate
							.queryForObject(EbMSMessageContextRowMapper.SELECT + " from ebms_message where message_id = ?", new EbMSMessageContextRowMapper(), messageId));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<EbMSMessageProperties> getEbMSMessagePropertiesByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		try
		{
			return Optional.of(
					jdbcTemplate.queryForObject(
							EbMSMessageContextRowMapper.SELECT
									+ " from ebms_message"
									+ " where cpa_id = ?"
									+ " and ref_to_message_id = ?"
									+ (actions.length == 0
											? ""
											: " and service = '"
													+ EbMSAction.EBMS_SERVICE_URI
													+ "' and action in ('"
													+ Arrays.stream(actions).map(EbMSAction::getAction).collect(Collectors.joining("','"))
													+ "')"),
							new EbMSMessageContextRowMapper(),
							cpaId,
							refToMessageId));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<Document> getDocument(String messageId)
	{
		try
		{
			return Optional
					.of(DOMUtils.read(jdbcTemplate.queryForObject("select content" + " from ebms_message" + " where message_id = ?", String.class, messageId)));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new DataRetrievalFailureException("", e);
		}
	}

	@Override
	public Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId)
	{
		try
		{
			val content = jdbcTemplate.queryForObject(
					"select content" + " from ebms_message" + " where message_id = ?" + " and (status is null or status = " + EbMSMessageStatus.CREATED.getId() + ")",
					String.class,
					messageId);
			val builder = EbMSDocument.builder().contentId(messageId).message(DOMUtils.read(content)).attachments(getAttachments(messageId, ebMSAttachmentRowMapper));
			return Optional.of(builder.build());
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new DataRetrievalFailureException("", e);
		}
	}

	@Override
	public Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions)
	{
		try
		{
			val document = jdbcTemplate.queryForObject(
					"select message_id, content"
							+ " from ebms_message"
							+ " where cpa_id = ?"
							+ " and ref_to_message_id = ?"
							+ (actions.length == 0
									? ""
									: " and service = '"
											+ EbMSAction.EBMS_SERVICE_URI
											+ "' and action in ('"
											+ Arrays.stream(actions).map(EbMSAction::getAction).collect(Collectors.joining("','"))
											+ "')"),
					new RowMapper<EbMSDocument>()
					{
						@Override
						public EbMSDocument mapRow(ResultSet rs, int rowNum) throws SQLException
						{
							try
							{
								return EbMSDocument.builder().contentId(rs.getString("message_id")).message(DOMUtils.read(rs.getString("content"))).build();
							}
							catch (ParserConfigurationException | SAXException | IOException e)
							{
								throw new SQLException(e);
							}
						}
					},
					cpaId,
					refToMessageId);
			val builder = EbMSDocument.builder()
					.contentId(document.getContentId())
					.message(document.getMessage())
					.attachments(getAttachments(refToMessageId, ebMSAttachmentRowMapper));
			return Optional.of(builder.build());
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<Instant> getPersistTime(String messageId)
	{
		return Optional.ofNullable(jdbcTemplate.queryForObject("select persist_time from ebms_message where message_id = ?", Timestamp.class, messageId))
				.map(Timestamp::toInstant);
	}

	@Override
	public Optional<EbMSAction> getMessageAction(String messageId)
	{
		try
		{
			return EbMSAction.get(jdbcTemplate.queryForObject("select action from ebms_message where message_id = ?", String.class, messageId));
		}
		catch (EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
	}

	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status)
	{
		val parameters = new ArrayList<Object>();
		return jdbcTemplate.queryForList(
				"select message_id"
						+ " from ebms_message"
						+ " where status = "
						+ status.getId()
						+ getMessageFilter(messageFilter, parameters)
						+ " order by time_stamp asc",
				String.class,
				parameters.toArray(new Object[0]));
	}

	@Override
	public List<String> getMessageIds(MessageFilter messageFilter, EbMSMessageStatus status, int maxNr)
	{
		val parameters = new ArrayList<Object>();
		val messageContextFilter = getMessageFilter(messageFilter, parameters);
		parameters.add(maxNr);
		return jdbcTemplate.queryForList(
				"select message_id"
						+ " from ebms_message"
						+ " where status = "
						+ status.getId()
						+ messageContextFilter
						+ " order by time_stamp asc"
						+ " offset 0 rows"
						+ " fetch first ? rows only",
				String.class,
				parameters.toArray(new Object[0]));
	}

	@Override
	public String insertMessage(
			final Instant timestamp,
			final Instant persistTime,
			final Document document,
			final EbMSBaseMessage message,
			final List<EbMSAttachment> attachments,
			final EbMSMessageStatus status)
	{
		val messageHeader = message.getMessageHeader();
		jdbcTemplate.update(c ->
		{
			try
			{
				val ps = c.prepareStatement(
						"insert into ebms_message ("
								+ "time_stamp,"
								+ "cpa_id,"
								+ "conversation_id,"
								+ "message_id,"
								+ "ref_to_message_id,"
								+ "time_to_live,"
								+ "from_party_id,"
								+ "from_role,"
								+ "to_party_id,"
								+ "to_role,"
								+ "service,"
								+ "action,"
								+ "content,"
								+ "status,"
								+ "status_time,"
								+ "persist_time"
								+ ") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
				ps.setTimestamp(1, Timestamp.from(timestamp));
				ps.setString(2, messageHeader.getCPAId());
				ps.setString(3, messageHeader.getConversationId());
				ps.setString(4, messageHeader.getMessageData().getMessageId());
				ps.setString(5, messageHeader.getMessageData().getRefToMessageId());
				ps.setTimestamp(6, messageHeader.getMessageData().getTimeToLive() == null ? null : Timestamp.from(messageHeader.getMessageData().getTimeToLive()));
				ps.setString(7, EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
				ps.setString(8, messageHeader.getFrom().getRole());
				ps.setString(9, EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
				ps.setString(10, messageHeader.getTo().getRole());
				ps.setString(11, EbMSMessageUtils.toString(messageHeader.getService()));
				ps.setString(12, messageHeader.getAction());
				ps.setString(13, DOMUtils.toString(document, "UTF-8"));
				ps.setObject(14, status != null ? status.getId() : null, java.sql.Types.INTEGER);
				ps.setTimestamp(15, status != null ? Timestamp.from(timestamp) : null);
				ps.setTimestamp(16, persistTime != null ? Timestamp.from(persistTime) : null);
				return ps;
			}
			catch (TransformerException e)
			{
				throw new SQLException(e);
			}
		});
		insertAttachments(messageHeader.getMessageData().getMessageId(), attachments);
		return messageHeader.getMessageData().getMessageId();
	}

	protected void insertAttachments(String messageId, List<EbMSAttachment> attachments) throws InvalidDataAccessApiUsageException
	{
		val orderNr = new AtomicInteger();
		jdbcTemplate.batchUpdate(
				"insert into ebms_attachment (message_id,order_nr,name,content_id,content_type,content) values (?,?,?,?,?,?)",
				new BatchPreparedStatementSetter()
				{
					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException
					{
						try
						{
							ps.setObject(1, messageId);
							ps.setInt(2, orderNr.getAndIncrement());
							ps.setString(3, attachments.get(i).getName());
							ps.setString(4, attachments.get(i).getContentId());
							ps.setString(5, attachments.get(i).getContentType());
							ps.setBinaryStream(6, attachments.get(i).getInputStream());
						}
						catch (IOException e)
						{
							throw new SQLException(e);
						}
					}

					@Override
					public int getBatchSize()
					{
						return attachments.size();
					}
				});
	}

	@Override
	public int updateMessage(String messageId, EbMSMessageStatus oldStatus, EbMSMessageStatus newStatus)
	{
		return jdbcTemplate.update(
				"update ebms_message" + " set status = ?," + " status_time = ?" + " where message_id = ?" + " and status = ?",
				newStatus.getId(),
				Timestamp.from(Instant.now()),
				messageId,
				oldStatus != null ? oldStatus.getId() : null);
	}

	@Override
	public int deleteAttachments(String messageId)
	{
		return jdbcTemplate.update("delete from ebms_attachment" + " where message_id = ?", messageId);
	}

	protected <T> List<T> getAttachments(String messageId, RowMapper<T> rowMapper)
	{
		return jdbcTemplate.query(
				"select name, content_id, content_type, content" + " from ebms_attachment" + " where message_id = ?" + " order by order_nr",
				rowMapper,
				messageId);
	}
}
