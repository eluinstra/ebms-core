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
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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
import org.apache.cxf.io.CachedOutputStream;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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

import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSEventStatus;
import nl.clockwork.ebms.Constants.EbMSMessageEventType;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.JAXBParser;
import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSAttachmentDataSource;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSEvent;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.model.EbMSMessageEvent;
import nl.clockwork.ebms.model.Role;
import nl.clockwork.ebms.model.URLMapping;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.util.EbMSMessageUtils;

public abstract class AbstractEbMSDAO implements EbMSDAO
{
	protected TransactionTemplate transactionTemplate;
	protected JdbcTemplate jdbcTemplate;
	protected String serverId;
	
	public AbstractEbMSDAO(TransactionTemplate transactionTemplate, JdbcTemplate jdbcTemplate, boolean identifyServer, String serverId)
	{
		this.transactionTemplate = transactionTemplate;
		this.jdbcTemplate = jdbcTemplate;
		if (identifyServer)
			this.serverId = serverId;
	}

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
				new Object[]{cpaId},
				Integer.class
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
			String result = jdbcTemplate.queryForObject(
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
				" from url" +
				" where source = ?",
				new Object[]{source},
				Integer.class
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
				" from url" +
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
				" from url" +
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
				"insert into url (" +
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
				"update url set" +
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
				"delete from url" +
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
	public boolean existsMessage(String messageId) throws DAOException
	{
		try
		{
			return jdbcTemplate.queryForObject(
				"select count(message_id)" +
				" from ebms_message" +
				" where message_id = ?" +
				" and message_nr = 0",
				new Object[]{messageId},
				Integer.class
			) > 0;
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public boolean existsIdenticalMessage(EbMSMessage message) throws DAOException
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
				new Object[]{
					message.getMessageHeader().getMessageData().getMessageId(),
					message.getMessageHeader().getCPAId()/*,
					message.getMessageHeader().getFrom().getRole(),
					message.getMessageHeader().getTo().getRole(),
					message.getMessageHeader().getService(),
					message.getMessageHeader().getAction()*/
				},
				Integer.class
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
			List<EbMSDataSource> dataSources = new ArrayList<>();
			List<EbMSAttachment> attachments = getAttachments(messageId);
			for (EbMSAttachment attachment: attachments)
				dataSources.add(new EbMSDataSource(attachment.getName(),attachment.getContentId(),attachment.getContentType(),IOUtils.toByteArray(attachment.getInputStream())));
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
			List<EbMSAttachment> attachments = getAttachments(messageId);
			List<EbMSDataSourceMTOM> dataSources = attachments.stream()
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
				" status" +
				" from ebms_message" + 
				" where message_id = ?" +
				" and message_nr = 0",
				new RowMapper<EbMSMessageContext>()
				{
					@Override
					public EbMSMessageContext mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						EbMSMessageContext result = new EbMSMessageContext();
						result.setCpaId(rs.getString("cpa_id"));
						result.setFromRole(new Role(rs.getString("from_party_id"),rs.getString("from_role")));
						result.setToRole(new Role(rs.getString("to_party_id"),rs.getString("to_role")));
						result.setService(rs.getString("service"));
						result.setAction(rs.getString("action"));
						result.setTimestamp(rs.getTimestamp("time_stamp"));
						result.setConversationId(rs.getString("conversation_id"));
						result.setMessageId(rs.getString("message_id"));
						result.setRefToMessageId(rs.getString("ref_to_message_id"));
						result.setMessageStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")));
						return result;
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
	public Optional<EbMSMessageContext> getMessageContextByRefToMessageId(String cpaId, String refToMessageId, Service service, String...actions) throws DAOException
	{
		try
		{
			return Optional.of(jdbcTemplate.queryForObject(
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
				" status" +
				" from ebms_message" + 
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')"),
				new RowMapper<EbMSMessageContext>()
				{
					@Override
					public EbMSMessageContext mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						EbMSMessageContext result = new EbMSMessageContext();
						result.setCpaId(rs.getString("cpa_id"));
						result.setFromRole(new Role(rs.getString("from_party_id"),rs.getString("from_role")));
						result.setToRole(new Role(rs.getString("to_party_id"),rs.getString("to_role")));
						result.setService(rs.getString("service"));
						result.setAction(rs.getString("action"));
						result.setTimestamp(rs.getTimestamp("time_stamp"));
						result.setConversationId(rs.getString("conversation_id"));
						result.setMessageId(rs.getString("message_id"));
						result.setRefToMessageId(rs.getString("ref_to_message_id"));
						result.setMessageStatus(rs.getObject("status") == null ? null : EbMSMessageStatus.values()[rs.getInt("status")]);
						return result;
					}
					
				},
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
			return Optional.of(new EbMSDocument(messageId,DOMUtils.read(
					jdbcTemplate.queryForObject(
							"select content" +
							" from ebms_message" +
							" where message_id = ?" +
							" and message_nr = 0" +
							" and (status is null or status = " + EbMSMessageStatus.SENDING.id() + ")",
							String.class,
							messageId
						)
			),getAttachments(messageId)));
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
	public Optional<EbMSDocument> getEbMSDocumentByRefToMessageId(String cpaId, String refToMessageId, Service service, String...actions) throws DAOException
	{
		try
		{
			EbMSDocument document = jdbcTemplate.queryForObject(
				"select message_id, content" +
				" from ebms_message" +
				" where cpa_id = ?" +
				" and ref_to_message_id = ?" +
				" and message_nr = 0" +
				(service == null ? "" : " and service = '" + EbMSMessageUtils.toString(service) + "'") +
				(actions.length == 0 ? "" : " and action in ('" + StringUtils.join(actions,"','") + "')"),
				new RowMapper<EbMSDocument>()
				{

					@Override
					public EbMSDocument mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						try
						{
							return new EbMSDocument(rs.getString("message_id"),DOMUtils.read(rs.getString("content")));
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
			return Optional.of(new EbMSDocument(document.getContentId(),document.getMessage(),getAttachments(refToMessageId)));
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
							return rs.getObject("status") == null ? null : rs.getInt("status");
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
							return rs.getObject("action") == null ? null : EbMSAction.get(rs.getString("action"));
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
	public List<String> getMessageIds(EbMSMessageContext messageContext, EbMSMessageStatus status) throws DAOException
	{
		try
		{
			List<Object> parameters = new ArrayList<>();
			return jdbcTemplate.queryForList(
					"select message_id" +
					" from ebms_message" +
					" where message_nr = 0" +
					" and status = " + status.id() +
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
			List<Object> parameters = new ArrayList<>();
			String messageContextFilter = getMessageContextFilter(messageContext,parameters);
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
	public void insertMessage(final Date timestamp, final Date persistTime, final EbMSMessage message, final EbMSMessageStatus status) throws DAOException
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
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
								new PreparedStatementCreator()
								{
									
									@Override
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
											ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
											MessageHeader messageHeader = message.getMessageHeader();
											ps.setString(2,messageHeader.getCPAId());
											ps.setString(3,messageHeader.getConversationId());
											ps.setString(4,messageHeader.getMessageData().getMessageId());
											ps.setString(5,messageHeader.getMessageData().getRefToMessageId());
											ps.setTimestamp(6,messageHeader.getMessageData().getTimeToLive() == null ? null : new Timestamp(messageHeader.getMessageData().getTimeToLive().getTime()));
											ps.setString(7,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
											ps.setString(8,messageHeader.getFrom().getRole());
											ps.setString(9,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
											ps.setString(10,messageHeader.getTo().getRole());
											ps.setString(11,EbMSMessageUtils.toString(messageHeader.getService()));
											ps.setString(12,messageHeader.getAction());
											ps.setString(13,DOMUtils.toString(message.getMessage(),"UTF-8"));
											if (status == null)
											{
												ps.setNull(14,java.sql.Types.INTEGER);
												ps.setNull(15,java.sql.Types.TIMESTAMP);
											}
											else
											{
												ps.setInt(14,status.id());
												ps.setTimestamp(15,new Timestamp(timestamp.getTime()));
											}
											if (persistTime == null)
												ps.setNull(16,java.sql.Types.DATE);
											else
												ps.setTimestamp(16,new Timestamp(persistTime.getTime()));
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
							insertAttachments(keyHolder,message.getAttachments());
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
	public void insertDuplicateMessage(final Date timestamp, final EbMSMessage message, boolean storeAttachments) throws DAOException
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
							KeyHolder keyHolder = new GeneratedKeyHolder();
							jdbcTemplate.update(
								new PreparedStatementCreator()
								{
									
									@Override
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
											ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
											MessageHeader messageHeader = message.getMessageHeader();
											ps.setString(2,messageHeader.getCPAId());
											ps.setString(3,messageHeader.getConversationId());
											ps.setString(4,messageHeader.getMessageData().getMessageId());
											ps.setString(5,messageHeader.getMessageData().getMessageId());
											ps.setString(6,messageHeader.getMessageData().getRefToMessageId());
											ps.setTimestamp(7,messageHeader.getMessageData().getTimeToLive() == null ? null : new Timestamp(messageHeader.getMessageData().getTimeToLive().getTime()));
											ps.setString(8,EbMSMessageUtils.toString(messageHeader.getFrom().getPartyId().get(0)));
											ps.setString(9,messageHeader.getFrom().getRole());
											ps.setString(10,EbMSMessageUtils.toString(messageHeader.getTo().getPartyId().get(0)));
											ps.setString(11,messageHeader.getTo().getRole());
											ps.setString(12,EbMSMessageUtils.toString(messageHeader.getService()));
											ps.setString(13,messageHeader.getAction());
											ps.setString(14,DOMUtils.toString(message.getMessage(),"UTF-8"));
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
							if (storeAttachments)
								insertAttachments(keyHolder,message.getAttachments());
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
		AtomicInteger orderNr = new AtomicInteger();
		attachments.forEach(attachment ->
			jdbcTemplate.update(
				new PreparedStatementCreator()
				{
					@Override
					public PreparedStatement createPreparedStatement(Connection connection) throws SQLException
					{
						try (EbMSAttachment a = attachment)
						{
							InputStream content = a.getInputStream();
							PreparedStatement ps = connection.prepareStatement
							(
								"insert into ebms_attachment (" +					"message_id," +
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
							//ps.setBytes(7,IOUtils.toByteArray(content));
							ps.setBlob(7,content);
							return ps;
						}
						catch (IOException e)
						{
							throw new SQLException(e);
						}
					}
				}
			)
		);
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
				newStatus.id(),
				new Date(),
				messageId,
				oldStatus != null ? oldStatus.id() : null
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}

	@Override
	public void updateMessages(final List<String> messageIds, final EbMSMessageStatus oldStatus, final EbMSMessageStatus newStatus) throws DAOException
	{
		try
		{
			jdbcTemplate.batchUpdate(
					"update ebms_message" +
					" set status = ?," +
					" status_time = ?" +
					" where message_id = ?" +
					" and message_nr = 0" +
					" and status = ?",
					new BatchPreparedStatementSetter()
					{
						@Override
						public void setValues(PreparedStatement ps, int row) throws SQLException
						{
							ps.setInt(1,newStatus.id());
							ps.setTimestamp(2,new Timestamp(new Date().getTime()));
							ps.setString(3,messageIds.get(row));
							if (oldStatus == null)
								ps.setNull(4,java.sql.Types.INTEGER);
							else
								ps.setInt(4,oldStatus.id());
						}

						@Override
						public int getBatchSize()
						{
							return messageIds.size();
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
	public void deleteAttachments(final List<String> messageIds)
	{
		try
		{
			jdbcTemplate.batchUpdate(
				"delete from ebms_attachment" +
				" where message_id = ?",
				new BatchPreparedStatementSetter()
				{
					@Override
					public void setValues(PreparedStatement ps, int row) throws SQLException
					{
						ps.setString(1,messageIds.get(row));
					}

					@Override
					public int getBatchSize()
					{
						return messageIds.size();
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
	public List<EbMSEvent> getEventsBefore(Date timestamp) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				"select cpa_id, channel_id, message_id, time_to_live, time_stamp, is_confidential, retries" +
				" from ebms_event" +
				" where time_stamp <= ?" +
				(serverId == null ? " and server_id is null" : " and server_id = '" + serverId + "'") +
				//" and (server_id = ? or (server_id is null and ? is null))" +
				" order by time_stamp asc",
				new RowMapper<EbMSEvent>()
				{
					@Override
					public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						return new EbMSEvent(rs.getString("cpa_id"),rs.getString("channel_id"),rs.getString("message_id"),rs.getTimestamp("time_to_live"),rs.getTimestamp("time_stamp"),rs.getBoolean("is_confidential"),rs.getInt("retries"));
					}
				},
				timestamp
			);
		}
		catch (DataAccessException e)
		{
			throw new DAOException(e);
		}
	}
	
	public abstract String getEventsBeforeQuery(int maxNr);

	@Override
	public List<EbMSEvent> getEventsBefore(Date timestamp, int maxNr) throws DAOException
	{
		try
		{
			return jdbcTemplate.query(
				getEventsBeforeQuery(maxNr),
				new RowMapper<EbMSEvent>()
				{
					@Override
					public EbMSEvent mapRow(ResultSet rs, int rowNum) throws SQLException
					{
						return new EbMSEvent(rs.getString("cpa_id"),rs.getString("channel_id"),rs.getString("message_id"),rs.getTimestamp("time_to_live"),rs.getTimestamp("time_stamp"),rs.getBoolean("is_confidential"),rs.getInt("retries"));
					}
				},
				timestamp
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
					"channel_id," +
					"message_id," +
					"time_to_live," +
					"time_stamp," +
					"is_confidential," +
					"retries," +
					"server_id" +
				") values (?,?,?,?,?,?,?,?)",
				event.getCpaId(),
				event.getDeliveryChannelId(),
				event.getMessageId(),
				event.getTimeToLive(),
				event.getTimestamp(),
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
				event.getTimestamp(),
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
	public void insertEventLog(String messageId, Date timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException
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
				timestamp,
				uri,
				status.id(),
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
				.mapToInt(e -> e.ordinal())
				.mapToObj(String::valueOf)
				.collect(Collectors.joining(delimiter));
	}
	
	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types) throws DAOException
	{
		try
		{
			List<Object> parameters = new ArrayList<>();
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
				new RowMapper<EbMSMessageEvent>()
				{
					@Override
					public EbMSMessageEvent mapRow(ResultSet rs, int nr) throws SQLException
					{
						return new EbMSMessageEvent(rs.getString("message_id"),EbMSMessageEventType.values()[rs.getInt("event_type")]);
					}
				}
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
			List<Object> parameters = new ArrayList<>();
			String messageContextFilter = getMessageContextFilter(messageContext,parameters);
			return jdbcTemplate.query(
				getMessageEventsQuery(messageContextFilter,types,maxNr),
				parameters.toArray(new Object[0]),
				new RowMapper<EbMSMessageEvent>()
				{
					@Override
					public EbMSMessageEvent mapRow(ResultSet rs, int nr) throws SQLException
					{
						return new EbMSMessageEvent(rs.getString("message_id"),EbMSMessageEventType.values()[rs.getInt("event_type")]);
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
				type.ordinal(),
				new Date()
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

	@Override
	public void processEbMSMessageEvents(final List<String> messageIds) throws DAOException
	{
		try
		{
			jdbcTemplate.batchUpdate(
				"update ebms_message_event" +
				" set procesed = 1," +
				" where message_id = ?",
				new BatchPreparedStatementSetter()
				{
					@Override
					public void setValues(PreparedStatement ps, int row) throws SQLException
					{
						ps.setString(1,messageIds.get(row));
					}

					@Override
					public int getBatchSize()
					{
						return messageIds.size();
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
	public Optional<Date> getPersistTime(String messageId)
	{
		return Optional.of(jdbcTemplate.queryForObject("select persist_time from ebms_message where message_id = ? and message_nr = 0",Date.class,messageId));
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
					Blob contentBlob = rs.getBlob("content");
					try
					{
						InputStream content = contentBlob.getBinaryStream();
						CachedOutputStream cachedOutputStream = new CachedOutputStream();
						CachedOutputStream.copyStream(content,cachedOutputStream,1024);
						cachedOutputStream.lockOutputStream();
						return new EbMSAttachmentDataSource(rs.getString("name"),rs.getString("content_id"),rs.getString("content_type"),cachedOutputStream);
					}
					catch (IOException e)
					{
						throw new EbMSProcessingException(e);
					}
					finally
					{
						contentBlob.free();
					}
				}
			},
			messageId
		);
	}

	protected String getMessageContextFilter(EbMSMessageContext messageContext, List<Object> parameters)
	{
		StringBuffer result = new StringBuffer();
		if (messageContext != null)
		{
			if (messageContext.getCpaId() != null)
			{
				parameters.add(messageContext.getCpaId());
				result.append(" and ebms_message.cpa_id = ?");
			}
			if (messageContext.getFromRole() != null)
			{
				if (messageContext.getFromRole().getPartyId() != null)
				{
					parameters.add(messageContext.getFromRole().getPartyId());
					result.append(" and ebms_message.from_party_id = ?");
				}
				if (messageContext.getFromRole().getRole() != null)
				{
					parameters.add(messageContext.getFromRole().getRole());
					result.append(" and ebms_message.from_role = ?");
				}
			}
			if (messageContext.getToRole() != null)
			{
				if (messageContext.getToRole().getPartyId() != null)
				{
					parameters.add(messageContext.getToRole().getPartyId());
					result.append(" and ebms_message.to_party_id = ?");
				}
				if (messageContext.getToRole().getRole() != null)
				{
					parameters.add(messageContext.getToRole().getRole());
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
				parameters.add(messageContext.getMessageStatus().ordinal());
				result.append(" and ebms_message.status = ?");
			}
		}
		return result.toString();
	}

}
