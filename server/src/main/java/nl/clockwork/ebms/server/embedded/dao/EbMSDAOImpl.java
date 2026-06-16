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
package nl.clockwork.ebms.server.embedded.dao;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.client.api.DeliveryTaskStatus;
import nl.clockwork.ebms.common.protocol.EbMSAction;
import nl.clockwork.ebms.common.protocol.EbMSMessageStatus;
import nl.clockwork.ebms.server.embedded.web.Utils;
import nl.clockwork.ebms.server.model.core.CPA;
import nl.clockwork.ebms.server.model.core.DeliveryLog;
import nl.clockwork.ebms.server.model.core.DeliveryTask;
import nl.clockwork.ebms.server.model.core.EbMSAttachment;
import nl.clockwork.ebms.server.model.core.EbMSMessage;
import nl.clockwork.ebms.server.model.embedded.web.EbMSMessageFilter;
import nl.clockwork.ebms.server.model.embedded.web.TimeUnit;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
@Transactional(transactionManager = "dataSourceTransactionManager")
public class EbMSDAOImpl implements EbMSDAO, WithMessageFilter
{
	private static final String MESSAGE_PROPERTY_COLUMNS = "time_stamp, cpa_id, conversation_id, message_id, ref_to_message_id, time_to_live,"
			+ " from_party_id, from_role, to_party_id, to_role, service, action, status, status_time";
	private static final String MESSAGE_COLUMNS = MESSAGE_PROPERTY_COLUMNS + ", content";

	private static final RowMapper<CPA> CPA_ROW_MAPPER = (rs, rowNum) -> new CPA(rs.getString("cpa_id"), rs.getString("cpa"));

	private static final RowMapper<EbMSMessage> MESSAGE_PROPERTY_ROW_MAPPER = (rs, rowNum) -> mapMessage(rs, false);
	private static final RowMapper<EbMSMessage> MESSAGE_ROW_MAPPER = (rs, rowNum) -> mapMessage(rs, true);

	private static final RowMapper<EbMSAttachment> ATTACHMENT_PROPERTY_ROW_MAPPER =
			(rs, rowNum) -> new EbMSAttachment(rs.getString("name"), rs.getString("content_id"), rs.getString("content_type"));
	private static final RowMapper<EbMSAttachment> ATTACHMENT_ROW_MAPPER = (rs, rowNum) ->
	{
		try
		{
			val content = new CachedOutputStream();
			CachedOutputStream.copyStream(rs.getBinaryStream("content"), content, 4096);
			content.lockOutputStream();
			return new EbMSAttachment(rs.getString("name"), rs.getString("content_id"), rs.getString("content_type"), content);
		}
		catch (IOException e)
		{
			throw new DataRetrievalFailureException("Failed to read attachment content", e);
		}
	};

	private static final RowMapper<DeliveryTask> DELIVERY_TASK_ROW_MAPPER = (rs, rowNum) -> new DeliveryTask(
			rs.getTimestamp("time_to_live") == null ? null : rs.getTimestamp("time_to_live").toInstant(),
			rs.getTimestamp("time_stamp").toInstant(),
			rs.getInt("retries"));

	private static final RowMapper<DeliveryLog> DELIVERY_LOG_ROW_MAPPER = (rs, rowNum) -> new DeliveryLog(
			rs.getTimestamp("time_stamp").toInstant(),
			rs.getString("uri"),
			DeliveryTaskStatus.get(rs.getInt("status")).orElseThrow(),
			rs.getString("error_message"));

	@NonNull
	JdbcTemplate jdbcTemplate;

	@Override
	public CPA findCPA(String cpaId)
	{
		try
		{
			return jdbcTemplate.queryForObject("select cpa_id, cpa from cpa where cpa_id = ?", CPA_ROW_MAPPER, cpaId);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
	}

	@Override
	public long countCPAs()
	{
		val result = jdbcTemplate.queryForObject("select count(cpa_id) from cpa", Long.class);
		return result == null ? 0L : result;
	}

	@Override
	public List<String> selectCPAIds()
	{
		return jdbcTemplate.queryForList("select cpa_id from cpa order by cpa_id asc", String.class);
	}

	@Override
	public List<CPA> selectCPAs(long first, long count)
	{
		return jdbcTemplate.query("select cpa_id, cpa from cpa order by cpa_id asc offset ? rows fetch first ? rows only", CPA_ROW_MAPPER, first, count);
	}

	@Override
	public EbMSMessage findMessage(String messageId)
	{
		EbMSMessage result;
		try
		{
			result = jdbcTemplate.queryForObject("select " + MESSAGE_COLUMNS + " from ebms_message where message_id = ?", MESSAGE_ROW_MAPPER, messageId);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
		if (result == null)
			return null;
		result.setAttachments(getAttachments(messageId));
		result.setDeliveryTask(getDeliveryTask(messageId));
		result.setDeliveryLogs(getDeliveryLogs(messageId));
		result.getAttachments().forEach(a -> a.setMessage(result));
		return result;
	}

	@Override
	public boolean existsResponseMessage(String messageId)
	{
		val count = jdbcTemplate.queryForObject(
				"select count(message_id) from ebms_message where ref_to_message_id = ? and service = ?",
				Integer.class,
				messageId,
				EbMSAction.EBMS_SERVICE_URI);
		return count != null && count > 0;
	}

	@Override
	public EbMSMessage findResponseMessage(String messageId)
	{
		EbMSMessage result;
		try
		{
			result = jdbcTemplate.queryForObject(
					"select " + MESSAGE_COLUMNS + " from ebms_message where ref_to_message_id = ? and service = ?",
					MESSAGE_ROW_MAPPER,
					messageId,
					EbMSAction.EBMS_SERVICE_URI);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
		if (result == null)
			return null;
		result.setDeliveryLogs(getDeliveryLogs(messageId));
		return result;
	}

	@Override
	public long countMessages(EbMSMessageFilter filter)
	{
		val parameters = new ArrayList<Object>();
		val sql = "select count(message_id) from ebms_message where 1 = 1" + getMessageFilter(filter, parameters);
		val result = jdbcTemplate.queryForObject(sql, Long.class, parameters.toArray());
		return result == null ? 0L : result;
	}

	@Override
	public List<EbMSMessage> selectMessages(EbMSMessageFilter filter, long first, long count)
	{
		val parameters = new ArrayList<Object>();
		val sql = "select "
				+ MESSAGE_PROPERTY_COLUMNS
				+ " from ebms_message where 1 = 1"
				+ getMessageFilter(filter, parameters)
				+ " order by time_stamp desc offset ? rows fetch first ? rows only";
		parameters.add(first);
		parameters.add(count);
		return jdbcTemplate.query(sql, MESSAGE_PROPERTY_ROW_MAPPER, parameters.toArray());
	}

	@Override
	public EbMSAttachment findAttachment(String messageId, String contentId)
	{
		val results = jdbcTemplate.query(
				"select name, content_id, content_type, content from ebms_attachment where message_id = ? and content_id = ? order by order_nr asc",
				ATTACHMENT_ROW_MAPPER,
				messageId,
				contentId);
		return results.isEmpty() ? null : results.get(0);
	}

	protected List<EbMSAttachment> getAttachments(String messageId)
	{
		return jdbcTemplate.query(
				"select name, content_id, content_type from ebms_attachment where message_id = ? order by order_nr asc",
				ATTACHMENT_PROPERTY_ROW_MAPPER,
				messageId);
	}

	private DeliveryTask getDeliveryTask(String messageId)
	{
		try
		{
			return jdbcTemplate
					.queryForObject("select time_to_live, time_stamp, retries from delivery_task where message_id = ?", DELIVERY_TASK_ROW_MAPPER, messageId);
		}
		catch (EmptyResultDataAccessException e)
		{
			return null;
		}
	}

	private List<DeliveryLog> getDeliveryLogs(String messageId)
	{
		return jdbcTemplate.query("select time_stamp, uri, status, error_message from delivery_log where message_id = ?", DELIVERY_LOG_ROW_MAPPER, messageId);
	}

	@Override
	public List<String> selectMessageIds(String cpaId, String fromRole, String toRole, EbMSMessageStatus...statuses)
	{
		val sql = "select message_id from ebms_message"
				+ " where cpa_id = ? and from_role = ? and to_role = ? and status in ("
				+ joinStatusIds(statuses)
				+ ")"
				+ " order by time_stamp desc";
		return jdbcTemplate.queryForList(sql, String.class, cpaId, fromRole, toRole);
	}

	@Override
	public Map<Integer, Integer> selectMessageTraffic(LocalDateTime from, LocalDateTime to, TimeUnit timeUnit, EbMSMessageStatus...statuses)
	{
		val sql = "select time_stamp from ebms_message"
				+ " where time_stamp >= ? and time_stamp < ?"
				+ (statuses.length == 0 ? " and status is not null" : " and status in (" + joinStatusIds(statuses) + ")");
		val timestamps = jdbcTemplate.queryForList(sql, Timestamp.class, Timestamp.from(toInstant(from)), Timestamp.from(toInstant(to)));
		val result = new HashMap<Integer, Integer>();
		for (val ts : timestamps)
		{
			val bucket = bucket(ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), timeUnit);
			if (bucket != null)
				result.merge(bucket, 1, Integer::sum);
		}
		return result;
	}

	@Override
	public void printMessagesToCSV(final CSVPrinter printer, EbMSMessageFilter filter)
	{
		val parameters = new ArrayList<Object>();
		val sql = "select " + MESSAGE_PROPERTY_COLUMNS + " from ebms_message where 1 = 1" + getMessageFilter(filter, parameters) + " order by time_stamp desc";
		jdbcTemplate.query(sql, rs ->
		{
			try
			{
				printer.print(rs.getString("message_id"));
				printer.print(rs.getString("ref_to_message_id"));
				printer.print(rs.getString("conversation_id"));
				printer.print(rs.getTimestamp("time_stamp"));
				printer.print(rs.getTimestamp("time_to_live"));
				printer.print(rs.getString("cpa_id"));
				printer.print(rs.getString("from_role"));
				printer.print(rs.getString("to_role"));
				printer.print(rs.getString("service"));
				printer.print(rs.getString("action"));
				printer.print(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")).orElse(null));
				printer.print(rs.getTimestamp("status_time"));
				printer.println();
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, parameters.toArray());
	}

	@Override
	public void writeMessageToZip(String messageId, final ZipOutputStream zip)
	{
		jdbcTemplate.query("select content from ebms_message where message_id = ?", rs ->
		{
			try
			{
				val entry = new ZipEntry("message.xml");
				zip.putNextEntry(entry);
				zip.write(rs.getString("content").getBytes());
				zip.closeEntry();
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, messageId);
		writeAttachmentsToZip(messageId, zip);
	}

	protected void writeAttachmentsToZip(String messageId, final ZipOutputStream zip)
	{
		jdbcTemplate.query("select name, content_id, content_type, content from ebms_attachment where message_id = ? order by order_nr asc", rs ->
		{
			try
			{
				val entry = new ZipEntry(
						"attachments/"
								+ (StringUtils.isEmpty(rs.getString("name"))
										? rs.getString("content_id") + Utils.getFileExtension(rs.getString("content_type"))
										: rs.getString("name")));
				entry.setComment("Content-Type: " + rs.getString("content_type"));
				zip.putNextEntry(entry);
				IOUtils.copy(rs.getBinaryStream("content"), zip);
				zip.closeEntry();
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, messageId);
	}

	private static EbMSMessage mapMessage(ResultSet rs, boolean withContent) throws SQLException
	{
		val message = new EbMSMessage(
				rs.getTimestamp("time_stamp").toInstant(),
				rs.getString("cpa_id"),
				rs.getString("conversation_id"),
				rs.getString("message_id"),
				rs.getString("ref_to_message_id"),
				rs.getTimestamp("time_to_live") == null ? null : rs.getTimestamp("time_to_live").toInstant(),
				rs.getString("from_party_id"),
				rs.getString("from_role"),
				rs.getString("to_party_id"),
				rs.getString("to_role"),
				rs.getString("service"),
				rs.getString("action"),
				rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")).orElse(null),
				rs.getTimestamp("status_time") == null ? null : rs.getTimestamp("status_time").toInstant());
		if (withContent)
			message.setContent(rs.getString("content"));
		return message;
	}

	private static java.time.Instant toInstant(LocalDateTime value)
	{
		return value.atZone(ZoneId.systemDefault()).toInstant();
	}

	private static Integer bucket(LocalDateTime ts, TimeUnit timeUnit)
	{
		switch (timeUnit)
		{
			case HOUR:
				return ts.getMinute();
			case DAY:
				return ts.getHour();
			case MONTH:
				return ts.getDayOfMonth();
			case YEAR:
				return ts.getMonthValue();
			default:
				return null;
		}
	}
}