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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
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
import nl.clockwork.ebms.cpa.CPADAO;
import nl.clockwork.ebms.cpa.CertificateMappingDAO;
import nl.clockwork.ebms.cpa.URLMappingDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventDAO;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;
import nl.clockwork.ebms.event.processor.EbMSEvent;
import nl.clockwork.ebms.event.processor.EbMSEventDAO;
import nl.clockwork.ebms.event.processor.EbMSEventStatus;
import nl.clockwork.ebms.jaxb.JAXBParser;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;
import nl.clockwork.ebms.service.cpa.url.URLMapping;
import nl.clockwork.ebms.service.model.EbMSDataSource;
import nl.clockwork.ebms.service.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContent;
import nl.clockwork.ebms.service.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;
import nl.clockwork.ebms.service.model.Party;
import nl.clockwork.ebms.util.DOMUtils;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
abstract class AbstractEbMSDAO implements EbMSDAO, CPADAO, URLMappingDAO, CertificateMappingDAO, EbMSEventDAO, EbMSMessageEventDAO
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
		EbMSDAO ebMSDAO;

		@Override
		public EbMSMessageContext mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return EbMSMessageContext.builder()
					.cpaId(rs.getString("cpa_id"))
					.fromParty(new Party(rs.getString("from_party_id"),rs.getString("from_role")))
					.toParty(new Party(rs.getString("to_party_id"),rs.getString("to_role")))
					.service(rs.getString("service"))
					.action(rs.getString("action"))
					.timestamp(ebMSDAO.toInstant(rs.getTimestamp("time_stamp")))
					.conversationId(rs.getString("conversation_id"))
					.messageId(rs.getString("message_id"))
					.refToMessageId(rs.getString("ref_to_message_id"))
					.messageStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")))
					.build();
		}
	}
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	@AllArgsConstructor
	private static class EbMSEventRowMapper implements RowMapper<EbMSEvent>
	{
		public static final String SELECT = "select cpa_id, send_channel_id, receive_channel_id, message_id, time_to_live, time_stamp, is_confidential, retries";
		EbMSDAO ebMSDAO;

		@Override
		public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
		{
			return EbMSEvent.builder()
					.cpaId(rs.getString("cpa_id"))
					.sendDeliveryChannelId(rs.getString("send_channel_id"))
					.receiveDeliveryChannelId(rs.getString("receive_channel_id"))
					.messageId(rs.getString("message_id"))
					.timeToLive(ebMSDAO.toInstant(rs.getTimestamp("time_to_live")))
					.timestamp(ebMSDAO.toInstant(rs.getTimestamp("time_stamp")))
					.isConfidential(rs.getBoolean("is_confidential"))
					.retries(rs.getInt("retries"))
					.build();
		}
	}
	public static class EbMSMessageEventRowMapper implements RowMapper<EbMSMessageEvent>
	{
		@Override
		public EbMSMessageEvent mapRow(ResultSet rs, int nr) throws SQLException
		{
			return new EbMSMessageEvent(rs.getString("message_id"),EbMSMessageEventType.values()[rs.getInt("event_type")]);
		}
	}

	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	JdbcTemplate jdbcTemplate;
	String serverId;
	
	@Override
	public void executeTransaction(final DAOTransactionCallback callback) throws DAOException
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
		catch (DataAccessException | TransactionException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsCPA(String cpaId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select count(*)" +
				" from cpa" +
				" where cpa_id = ?",
				Integer.class,
				cpaId
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Optional<CollaborationProtocolAgreement> getCPA(String cpaId) throws DAOException
	{
		try
		{
			val result = jdbcTemplate.queryForObject(
				"select cpa" +
				" from cpa" +
				" where cpa_id = ?",
				String.class,
				cpaId
			);
			return Optional.of(JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(result));
		}
		catch(EmptyResultDataAccessException e)
		{
			return Optional.empty();
		}
		catch (DataAccessException | JAXBException e)
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
	public void insertCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into cpa (" +
					"cpa_id," +
					"cpa" +
				") values (?,?)",
				cpa.getCpaid(),
				JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa)
			);
		}
		catch (DataAccessException | JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int updateCPA(CollaborationProtocolAgreement cpa) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"update cpa set" +
				" cpa = ?" +
				" where cpa_id = ?",
				JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cpa),
				cpa.getCpaid()
			);
		}
		catch (DataAccessException | JAXBException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int deleteCPA(String cpaId) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"delete from cpa" +
				" where cpa_id = ?",
				cpaId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsURLMapping(String source) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select count(*)" +
				" from url_mapping" +
				" where source = ?",
				Integer.class,
				source
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<String> getURLMapping(String source)
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				"select destination" +
				" from url_mapping" +
				" where source = ?",
				String.class,
				source
			));
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
	public List<URLMapping> getURLMappings() throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select source, destination" +
				" from url_mapping" +
				" order by source asc",
				new RowMapper<URLMapping>()
				{
					@Override
					public URLMapping mapRow(ResultSet rs, int nr) throws SQLException
					{
						return new URLMapping(rs.getString("source"),rs.getString("destination"));
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
	public void insertURLMapping(URLMapping urlMapping) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into url_mapping (" +
					"source," +
					"destination" +
				") values (?,?)",
				urlMapping.getSource(),
				urlMapping.getDestination()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int updateURLMapping(URLMapping urlMapping)
	{
		try
		{
			return jdbcTemplate.update
			(
				"update url_mapping set" +
				" destination = ?" +
				" where source = ?",
				urlMapping.getDestination(),
				urlMapping.getSource()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int deleteURLMapping(String source)
	{
		try
		{
			return jdbcTemplate.update
			(
				"delete from url_mapping" +
				" where source = ?",
				source
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsCertificateMapping(String id, String cpaId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select count(*)" +
				" from certificate_mapping" +
				" where id = ?" +
				" and (cpa_id = ? or (cpa_id is null and ? is null))",
				Integer.class,
				id,
				cpaId,
				cpaId
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<X509Certificate> getCertificateMapping(String id, String cpaId) throws DAOException
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				"select destination" +
				" from certificate_mapping" +
				" where id = ?" +
				" and (cpa_id = ? or (cpa_id is null and ? is null))",
				new RowMapper<X509Certificate>()
				{
					@Override
					public X509Certificate mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
							return (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("source"));
						}
						catch (CertificateException e)
						{
							throw new SQLException(e);
						}
					}
				},
				id,
				cpaId,
				cpaId
			));
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
	public List<CertificateMapping> getCertificateMappings() throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select source, destination, cpa_id" +
				" from certificate_mapping",
				new RowMapper<CertificateMapping>()
				{
					@Override
					public CertificateMapping mapRow(ResultSet rs, int nr) throws SQLException
					{
						try
						{
							val certificateFactory = CertificateFactory.getInstance("X509");
							val source = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("source"));
							val destination = (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream("destination"));
							val cpaId = rs.getString("cpa_id");
							return new CertificateMapping(source,destination,cpaId);
						}
						catch (CertificateException e)
						{
							throw new SQLException(e);
						}
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
	public void insertCertificateMapping(String id, CertificateMapping mapping) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into certificate_mapping (" +
					"id," +
					"source," +
					"destination," +
					"cpa_id" +
				") values (?,?,?,?)",
				id,
				mapping.getSource().getEncoded(),
				mapping.getDestination().getEncoded(),
				mapping.getCpaId()
			);
		}
		catch (CertificateEncodingException | DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int updateCertificateMapping(String id, CertificateMapping mapping) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"update certificate_mapping set" +
				" destination = ?" +
				" where id = ?" +
				" and (cpa_id = ? or (cpa_id is null and ? is null))",
				mapping.getDestination().getEncoded(),
				id,
				mapping.getCpaId(),
				mapping.getCpaId()
			);
		}
		catch (CertificateEncodingException | DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int deleteCertificateMapping(String id, String cpaId) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"delete from certificate_mapping" +
				" where id = ?" +
				" and (cpa_id = ? or (cpa_id is null and ? is null))",
				id,
				cpaId,
				cpaId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsMessage(String messageId) throws DAOException
	{
		try
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsIdenticalMessage(EbMSBaseMessage message) throws DAOException
	{
		try
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public Optional<EbMSMessageContent> getMessageContent(String messageId) throws DAOException
	{
		try
		{
			val dataSources = new ArrayList<EbMSDataSource>();
			val attachments = getAttachments(messageId);
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
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
				EbMSMessageContextRowMapper.SELECT +
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = 0",
				new EbMSMessageContextRowMapper(this),
				messageId
			));
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
	public Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, EbMSAction...actions) throws DAOException
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
				new EbMSMessageContextRowMapper(this),
				cpaId,
				refToMessageId
			));
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
	public Optional<Document> getDocument(String messageId) throws DAOException
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
		catch (DataAccessException | ParserConfigurationException | SAXException | IOException  e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public Optional<EbMSDocument> getEbMSDocumentIfUnsent(String messageId) throws DAOException
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
		try
		{
			return Optional.of(EbMSMessageStatus.get(
				jdbcTemplate.queryForObject(
					"select status" +
					" from ebms_message" +
					" where message_id = ?" +
					" and message_nr = 0",
					new RowMapper<Integer>()
					{
						@Override
						public Integer mapRow(ResultSet rs, int rowNum) throws SQLException
						{
							return rs.getObject("status",Integer.class);
						}
					},
					messageId
				)
			));
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
	public Optional<EbMSAction> getMessageAction(String messageId) throws DAOException
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
					"select action" +
					" from ebms_message" +
					" where message_id = ?" +
					" and message_nr = 0",
					new RowMapper<EbMSAction>()
					{
						@Override
						public EbMSAction mapRow(ResultSet rs, int rowNum) throws SQLException
						{
							return EbMSAction.get(rs.getString("action"));
						}
					},
					messageId
				));
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
	public Optional<Instant> getPersistTime(String messageId)
	{
		return Optional.ofNullable(toInstant(jdbcTemplate.queryForObject("select persist_time from ebms_message where message_id = ? and message_nr = 0",Timestamp.class,messageId)));
	}

	@Override
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException
	{
		try
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
			val parameters = new ArrayList<Object>();
			val messageContextFilter = getMessageContextFilter(messageContext,parameters);
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
		try
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
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void deleteAttachments(String messageId)
	{
		try
		{
			jdbcTemplate.update(
				"delete from ebms_attachment" +
				" where message_id = ?",
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				EbMSEventRowMapper.SELECT +
				" from ebms_event" +
				" where time_stamp <= ?" +
				(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
				//" and (server_id = ? or (server_id is null and ? is null))" +
				" order by time_stamp asc",
				new EbMSEventRowMapper(this),
				Timestamp.from(timestamp)
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	public abstract String getEventsBeforeQuery(int maxNr);

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp, int maxNr) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				getEventsBeforeQuery(maxNr),
				new EbMSEventRowMapper(this),
				Timestamp.from(timestamp)
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
			jdbcTemplate.update(
				"insert into ebms_event (" +
					"cpa_id," +
					"send_channel_id," +
					"receive_channel_id," +
					"message_id," +
					"time_to_live," +
					"time_stamp," +
					"is_confidential," +
					"retries," +
					"server_id" +
				") values (?,?,?,?,?,?,?,?,?)",
				event.getCpaId(),
				event.getSendDeliveryChannelId(),
				event.getReceiveDeliveryChannelId(),
				event.getMessageId(),
				event.getTimeToLive() != null ? Timestamp.from(event.getTimeToLive()) : null,
				Timestamp.from(event.getTimestamp()),
				event.isConfidential(),
				event.getRetries(),
				serverId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void updateEvent(EbMSEvent event) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"update ebms_event set" +
				" time_stamp = ?," +
				" retries = ?" +
				" where message_id = ?",
				Timestamp.from(event.getTimestamp()),
				event.getRetries(),
				event.getMessageId()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	@Override
	public void deleteEvent(String messageId) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"delete from ebms_event" +
				" where message_id = ?",
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException
	{
		try
		{
			jdbcTemplate.update(
				"insert into ebms_event_log (" +
					"message_id," +
					"time_stamp," +
					"uri," +
					"status," +
					"error_message" +
				") values (?,?,?,?,?)",
				messageId,
				Timestamp.from(timestamp),
				uri,
				status.getId(),
				errorMessage
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	protected String join(EbMSMessageEventType[] array, String delimiter)
	{
		return Stream.of(array)
				.mapToInt(e -> e.getId())
				.mapToObj(String::valueOf)
				.collect(Collectors.joining(delimiter));
	}
	
	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types) throws DAOException
	{
		try
		{
			val parameters = new ArrayList<Object>();
			return jdbcTemplate.query(
				"select ebms_message_event.message_id, ebms_message_event.event_type" +
				" from ebms_message_event, ebms_message" +
				" where ebms_message_event.processed = 0" +
				" and ebms_message_event.event_type in (" + join(types == null ? EbMSMessageEventType.values() : types,",") + ")" +
				" and ebms_message_event.message_id = ebms_message.message_id" +
				" and ebms_message.message_nr = 0" +
				getMessageContextFilter(messageContext,parameters) +
				" order by ebms_message.time_stamp asc",
				parameters.toArray(new Object[0]),
				new EbMSMessageEventRowMapper()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	protected abstract String getMessageEventsQuery(String messageContextFilter, EbMSMessageEventType[] types, int maxNr);

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr) throws DAOException
	{
		try
		{
			val parameters = new ArrayList<Object>();
			val messageContextFilter = getMessageContextFilter(messageContext,parameters);
			return jdbcTemplate.query(
				getMessageEventsQuery(messageContextFilter,types,maxNr),
				parameters.toArray(new Object[0]),
				new EbMSMessageEventRowMapper()
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void insertEbMSMessageEvent(String messageId, EbMSMessageEventType type) throws DAOException
	{
		try
		{
			jdbcTemplate.update
			(
				"insert into ebms_message_event (" +
					"message_id," +
					"event_type," +
					"time_stamp" +
				") values (?,?,?)",
				messageId,
				type.getId(),
				Timestamp.from(Instant.now())
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public int processEbMSMessageEvent(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.update
			(
				"update ebms_message_event" +
				" set processed = 1" +
				" where message_id = ?",
				messageId
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	protected List<EbMSAttachment> getAttachments(String messageId)
	{
		return jdbcTemplate.query(
			"select name, content_id, content_type, content" + 
			" from ebms_attachment" + 
			" where message_id = ?" +
			" and message_nr = 0" +
			" order by order_nr",
			new RowMapper<EbMSAttachment>()
			{
				@Override
				public EbMSAttachment mapRow(ResultSet rs, int rowNum) throws SQLException
				{
					try
					{
						return EbMSAttachmentFactory.createCachedEbMSAttachment(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),rs.getBinaryStream("content"));
					}
					catch (IOException e)
					{
						throw new EbMSProcessingException(e);
					}
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
