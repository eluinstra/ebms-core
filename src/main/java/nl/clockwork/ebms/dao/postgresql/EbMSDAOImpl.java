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
import java.sql.Timestamp;
import java.util.Date;

import javax.xml.bind.JAXBException;

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.Constants.EbMSAction;
import nl.clockwork.ebms.Constants.EbMSMessageStatus;
import nl.clockwork.ebms.common.util.XMLMessageBuilder;
import nl.clockwork.ebms.dao.AbstractEbMSDAO;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSAttachment;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.ebxml.AckRequested;
import nl.clockwork.ebms.model.ebxml.Acknowledgment;
import nl.clockwork.ebms.model.ebxml.ErrorList;
import nl.clockwork.ebms.model.ebxml.Manifest;
import nl.clockwork.ebms.model.ebxml.MessageHeader;
import nl.clockwork.ebms.model.ebxml.MessageOrder;
import nl.clockwork.ebms.model.ebxml.StatusRequest;
import nl.clockwork.ebms.model.ebxml.StatusResponse;
import nl.clockwork.ebms.model.ebxml.SyncReply;
import nl.clockwork.ebms.model.xml.dsig.SignatureType;

import org.apache.commons.io.IOUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class EbMSDAOImpl extends AbstractEbMSDAO
{
	public class IdExtractor implements ResultSetExtractor<Long>
	{

		@Override
		public Long extractData(ResultSet rs) throws SQLException, DataAccessException
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
	public long insertMessage(final Date timestamp, final EbMSMessage message, final EbMSMessageStatus status) throws DAOException
	{
		try
		{
			return transactionTemplate.execute(
				new TransactionCallback<Long>()
				{
					@Override
					public Long doInTransaction(TransactionStatus arg0)
					{
						try
						{
							Long key = (Long)jdbcTemplate.query(
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
													//"error_list," +
													//"acknowledgment," +
													//"manifest," +
													//"status_request," +
													//"status_response," +
													"status," +
													"status_time" +
												") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
												//") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + (status == null ? "null" : getTimestampFunction()) + ")",
												//new String[]{"id"}
												new int[]{1}
											);
											//ps.setDate(1,new java.sql.Date(timestamp.getTime()));
											//ps.setString(1,String.format(getDateFormat(),timestamp));
											ps.setTimestamp(1,new Timestamp(timestamp.getTime()));
											//ps.setObject(1,timestamp,Types.TIMESTAMP);
											//ps.setObject(1,timestamp);
											MessageHeader messageHeader = message.getMessageHeader();
											ps.setString(2,messageHeader.getCPAId());
											ps.setString(3,messageHeader.getConversationId());
											if (message.getMessageOrder() == null || message.getMessageOrder().getSequenceNumber() == null)
												ps.setNull(4,java.sql.Types.BIGINT);
											else
												ps.setLong(4,message.getMessageOrder().getSequenceNumber().getValue().longValue());
											ps.setString(5,messageHeader.getMessageData().getMessageId());
											ps.setString(6,messageHeader.getMessageData().getRefToMessageId());
											ps.setString(7,messageHeader.getFrom().getRole());
											ps.setString(8,messageHeader.getTo().getRole());
											ps.setString(9,messageHeader.getService().getType());
											ps.setString(10,messageHeader.getService().getValue());
											ps.setString(11,messageHeader.getAction());
											ps.setBytes(12,message.getOriginal());
											ps.setString(13,XMLMessageBuilder.getInstance(SignatureType.class).handle(message.getSignature()));
											ps.setString(14,XMLMessageBuilder.getInstance(MessageHeader.class).handle(messageHeader));
											ps.setString(15,XMLMessageBuilder.getInstance(SyncReply.class).handle(message.getSyncReply()));
											ps.setString(16,XMLMessageBuilder.getInstance(MessageOrder.class).handle(message.getMessageOrder()));
											ps.setString(17,XMLMessageBuilder.getInstance(AckRequested.class).handle(message.getAckRequested()));
											if (!Constants.EBMS_SERVICE_URI.equals(messageHeader.getService().getValue()))
												ps.setString(18,XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest()));
											else if (EbMSAction.MESSAGE_ERROR.action().equals(messageHeader.getAction()))
												ps.setString(18,XMLMessageBuilder.getInstance(ErrorList.class).handle(message.getErrorList()));
											else if (EbMSAction.ACKNOWLEDGMENT.action().equals(messageHeader.getAction()))
												ps.setString(18,XMLMessageBuilder.getInstance(Acknowledgment.class).handle(message.getAcknowledgment()));
											else if (EbMSAction.STATUS_REQUEST.action().equals(messageHeader.getAction()))
												ps.setString(18,XMLMessageBuilder.getInstance(StatusRequest.class).handle(message.getStatusRequest()));
											else if (EbMSAction.STATUS_RESPONSE.action().equals(messageHeader.getAction()))
												ps.setString(18,XMLMessageBuilder.getInstance(StatusResponse.class).handle(message.getStatusResponse()));
											else
												ps.setString(18,null);
											if (status == null)
												ps.setNull(19,java.sql.Types.INTEGER);
											else
												ps.setInt(19,status.id());
											//ps.setString(18,XMLMessageBuilder.getInstance(ErrorList.class).handle(message.getErrorList()));
											//ps.setString(19,XMLMessageBuilder.getInstance(Acknowledgment.class).handle(message.getAcknowledgment()));
											//ps.setString(20,XMLMessageBuilder.getInstance(Manifest.class).handle(message.getManifest()));
											//ps.setString(21,XMLMessageBuilder.getInstance(StatusRequest.class).handle(message.getStatusRequest()));
											//ps.setString(22,XMLMessageBuilder.getInstance(StatusResponse.class).handle(message.getStatusResponse()));
											//if (status == null)
												//ps.setNull(23,java.sql.Types.INTEGER);
											//else
												//ps.setInt(23,status.id());
											//ps.setString(24,status == null ? null : String.format(getDateFormat(),timestamp));
											//ps.setTimestamp(24,status == null ? null : new Timestamp(timestamp.getTime()));
											//ps.setObject(24,status == null ? null : timestamp,Types.TIMESTAMP);
											//ps.setObject(24,status == null ? null : timestamp);
											return ps;
										}
										catch (JAXBException e)
										{
											throw new SQLException(e);
										}
									}
								},
								new IdExtractor()
							);
					
							for (EbMSAttachment attachment : message.getAttachments())
							{
								jdbcTemplate.update
								(
									"insert into ebms_attachment (" +
										"ebms_message_id," +
										"name," +
										"content_id," +
										"content_type," +
										"content" +
									") values (?,?,?,?,?)",
									key,
									attachment.getName() == null ? Constants.DEFAULT_FILENAME : attachment.getName(),
									attachment.getContentId(),
									attachment.getContentType().split(";")[0].trim(),
									IOUtils.toByteArray(attachment.getInputStream())
								);
							}
							
							return key;
						}
						catch (Exception e)
						{
							throw new DAOException(e);
						}
					}
				}
			);
		}
		catch (Exception e)
		{
			throw new DAOException();
		}
	}
	
}
