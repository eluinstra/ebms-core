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

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.sql.SQLQueryFactory;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;
import nl.clockwork.ebms.common.EbMSAction;
import nl.clockwork.ebms.common.EbMSMessageStatus;
import nl.clockwork.ebms.querydsl.model.QCpa;
import nl.clockwork.ebms.querydsl.model.QDeliveryLog;
import nl.clockwork.ebms.querydsl.model.QDeliveryTask;
import nl.clockwork.ebms.querydsl.model.QEbmsAttachment;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.server.embedded.model.CPA;
import nl.clockwork.ebms.server.embedded.model.DeliveryLog;
import nl.clockwork.ebms.server.embedded.model.DeliveryTask;
import nl.clockwork.ebms.server.embedded.model.EbMSAttachment;
import nl.clockwork.ebms.server.embedded.model.EbMSMessage;
import nl.clockwork.ebms.server.embedded.web.Utils;
import nl.clockwork.ebms.server.embedded.web.message.EbMSMessageFilter;
import nl.clockwork.ebms.server.embedded.web.message.TimeUnit;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@AllArgsConstructor
@Transactional(transactionManager = "dataSourceTransactionManager")
public class EbMSDAOImpl implements EbMSDAO, WithMessageFilter
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QCpa cpaTable = QCpa.cpa1;
	QEbmsMessage messageTable = QEbmsMessage.ebmsMessage;
	QEbmsAttachment attachmentTable = QEbmsAttachment.ebmsAttachment;
	QDeliveryTask deliveryTaskTable = QDeliveryTask.deliveryTask;
	QDeliveryLog deliveryLogTable = QDeliveryLog.deliveryLog;
	Expression<?>[] ebMSMessageColumns = {messageTable.timeStamp, messageTable.cpaId, messageTable.conversationId, messageTable.messageId,
			messageTable.refToMessageId, messageTable.timeToLive, messageTable.fromPartyId, messageTable.fromRole, messageTable.toPartyId, messageTable.toRole,
			messageTable.service, messageTable.action, messageTable.content, messageTable.status, messageTable.statusTime};
	Expression<?>[] ebMSMessagePropertyColumns = {messageTable.timeStamp, messageTable.cpaId, messageTable.conversationId, messageTable.messageId,
			messageTable.refToMessageId, messageTable.timeToLive, messageTable.fromPartyId, messageTable.fromRole, messageTable.toPartyId, messageTable.toRole,
			messageTable.service, messageTable.action, messageTable.status, messageTable.statusTime};
	ConstructorExpression<CPA> cpaProjection = Projections.constructor(CPA.class, cpaTable.cpaId, cpaTable.cpa);
	ConstructorExpression<EbMSMessage> ebMSMessageProjection = Projections.constructor(EbMSMessage.class, ebMSMessageColumns);
	ConstructorExpression<EbMSMessage> ebMSMessagePropertyProjection = Projections.constructor(EbMSMessage.class, ebMSMessagePropertyColumns);
	ConstructorExpression<EbMSAttachment> ebMSAttachmentProjection =
			Projections.constructor(EbMSAttachment.class, attachmentTable.name, attachmentTable.contentId, attachmentTable.contentType, attachmentTable.content);
	ConstructorExpression<EbMSAttachment> ebMSAttachmentPropertiesProjection =
			Projections.constructor(EbMSAttachment.class, attachmentTable.name, attachmentTable.contentId, attachmentTable.contentType);
	ConstructorExpression<DeliveryTask> deliveryTaskProjection =
			Projections.constructor(DeliveryTask.class, deliveryTaskTable.timeToLive, deliveryTaskTable.timeStamp, deliveryTaskTable.retries);
	ConstructorExpression<DeliveryLog> deliveryLogProjection =
			Projections.constructor(DeliveryLog.class, deliveryLogTable.timeStamp, deliveryLogTable.uri, deliveryLogTable.status, deliveryLogTable.errorMessage);

	@Override
	public CPA findCPA(String cpaId)
	{
		return queryFactory.select(cpaProjection).from(cpaTable).where(cpaTable.cpaId.eq(cpaId)).fetchOne();
	}

	@Override
	public long countCPAs()
	{
		return queryFactory.select(cpaTable.cpaId.count()).from(cpaTable).fetchOne();
	}

	@Override
	public List<String> selectCPAIds()
	{
		return queryFactory.select(cpaTable.cpaId).from(cpaTable).orderBy(cpaTable.cpaId.asc()).fetch();
	}

	@Override
	public List<CPA> selectCPAs(long first, long count)
	{
		return queryFactory.select(cpaProjection).from(cpaTable).orderBy(cpaTable.cpaId.asc()).limit(count).offset(first).fetch();
	}

	@Override
	public EbMSMessage findMessage(String messageId)
	{
		val result = queryFactory.select(ebMSMessageProjection).from(messageTable).where(messageTable.messageId.eq(messageId)).fetchOne();
		result.setAttachments(getAttachments(messageId));
		result.setDeliveryTask(getDeliveryTask(messageId));
		result.setDeliveryLogs(getDeliveryLogs(messageId));
		result.getAttachments().forEach(a -> a.setMessage(result));
		return result;
	}

	@Override
	public boolean existsResponseMessage(String messageId)
	{
		return queryFactory.select(messageTable.messageId.count())
				.from(messageTable)
				.where(messageTable.refToMessageId.eq(messageId).and(messageTable.service.eq(EbMSAction.EBMS_SERVICE_URI)))
				.fetchOne()
				> 0;
	}

	@Override
	public EbMSMessage findResponseMessage(String messageId)
	{
		val result = queryFactory.select(ebMSMessageProjection)
				.from(messageTable)
				.where(messageTable.refToMessageId.eq(messageId).and(messageTable.service.eq(EbMSAction.EBMS_SERVICE_URI)))
				.fetchOne();
		result.setDeliveryLogs(getDeliveryLogs(messageId));
		return result;
	}

	@Override
	public long countMessages(EbMSMessageFilter filter)
	{
		return queryFactory.select(messageTable.messageId.count())
				.from(messageTable)
				.where(getMessageFilter(messageTable, filter, new BooleanBuilder()))
				.fetchOne();
	}

	@Override
	public List<EbMSMessage> selectMessages(EbMSMessageFilter filter, long first, long count)
	{
		return queryFactory.select(ebMSMessagePropertyProjection)
				.from(messageTable)
				.where(getMessageFilter(messageTable, filter, new BooleanBuilder()))
				.orderBy(messageTable.timeStamp.desc())
				.limit(count)
				.offset(first)
				.fetch();
	}

	@Override
	public EbMSAttachment findAttachment(String messageId, String contentId)
	{
		return queryFactory.select(ebMSAttachmentProjection)
				.from(attachmentTable)
				.where(attachmentTable.messageId.eq(messageId).and(attachmentTable.contentId.eq(contentId)))
				.orderBy(attachmentTable.orderNr.asc())
				.fetchOne();
	}

	protected List<EbMSAttachment> getAttachments(String messageId)
	{
		return queryFactory.select(ebMSAttachmentPropertiesProjection).from(attachmentTable).where(attachmentTable.messageId.eq(messageId)).fetch();
	}

	private DeliveryTask getDeliveryTask(String messageId)
	{
		return queryFactory.select(deliveryTaskProjection).from(deliveryTaskTable).where(deliveryTaskTable.messageId.eq(messageId)).fetchOne();
	}

	private List<DeliveryLog> getDeliveryLogs(String messageId)
	{
		return queryFactory.select(deliveryLogProjection).from(deliveryLogTable).where(deliveryLogTable.messageId.eq(messageId)).fetch();
	}

	@Override
	public List<String> selectMessageIds(String cpaId, String fromRole, String toRole, EbMSMessageStatus...statuses)
	{
		return queryFactory.select(messageTable.messageId)
				.from(messageTable)
				.where(messageTable.cpaId.eq(cpaId).and(messageTable.fromRole.eq(fromRole)).and(messageTable.toRole.eq(toRole)).and(messageTable.status.in(statuses)))
				.orderBy(messageTable.timeStamp.desc())
				.fetch();
	}

	@Override
	public Map<Integer, Integer> selectMessageTraffic(LocalDateTime from, LocalDateTime to, TimeUnit timeUnit, EbMSMessageStatus...statuses)
	{
		val result = queryFactory.select(getTimestamp(messageTable.timeStamp, timeUnit).as("time"), messageTable.messageId.count().as("nr"))
				.from(messageTable)
				.where(
						messageTable.timeStamp.goe(from.atZone(ZoneId.systemDefault()).toInstant())
								.and(messageTable.timeStamp.lt(to.atZone(ZoneId.systemDefault()).toInstant()))
								.and(statuses.length == 0 ? messageTable.status.isNotNull() : messageTable.status.in(statuses)))
				.groupBy(getTimestamp(messageTable.timeStamp, timeUnit))
				.fetch();
		return result.stream().collect(Collectors.toMap(t -> t.get(0, Integer.class), t -> t.get(1, Long.class).intValue()));
	}

	@Override
	public void printMessagesToCSV(final CSVPrinter printer, EbMSMessageFilter filter)
	{
		val query = queryFactory.select(ebMSMessagePropertyColumns)
				.from(messageTable)
				.where(getMessageFilter(messageTable, filter, new BooleanBuilder()))
				.orderBy(messageTable.timeStamp.desc())
				.getSQL();
		val sql = Objects.requireNonNull(query.getSQL());
		jdbcTemplate.query(sql, (rs, rowNum) ->
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
				printer.print(rs.getObject("status") == null ? null : EbMSMessageStatus.get(rs.getInt("status")));
				printer.print(rs.getTimestamp("status_time"));
				printer.println();
				return null;
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, query.getNullFriendlyBindings().toArray());
	}

	@Override
	public void writeMessageToZip(String messageId, final ZipOutputStream zip)
	{
		val query = queryFactory.select(messageTable.content).from(messageTable).where(messageTable.messageId.eq(messageId)).getSQL();
		val sql = Objects.requireNonNull(query.getSQL());
		jdbcTemplate.query(sql, (rs, rowNum) ->
		{
			try
			{
				val entry = new ZipEntry("message.xml");
				zip.putNextEntry(entry);
				zip.write(rs.getString("content").getBytes());
				zip.closeEntry();
				return null;
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, query.getNullFriendlyBindings().toArray());
		writeAttachmentsToZip(messageId, zip);
	}

	protected void writeAttachmentsToZip(String messageId, final ZipOutputStream zip)
	{
		val query = queryFactory.select(attachmentTable.name, attachmentTable.contentId, attachmentTable.contentType, attachmentTable.content)
				.from(attachmentTable)
				.where(attachmentTable.messageId.eq(messageId))
				.getSQL();
		val sql = Objects.requireNonNull(query.getSQL());
		jdbcTemplate.query(sql, (rs, rowNum) ->
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
				return null;
			}
			catch (IOException e)
			{
				throw new SQLException(e);
			}
		}, query.getNullFriendlyBindings().toArray());
	}

	protected BooleanBuilder getMessageFilter(QEbmsMessage table, EbMSMessageFilter messageFilter, BooleanBuilder builder)
	{
		builder = applyFilter(table, messageFilter, builder);
		if (messageFilter != null)
		{
			if (!messageFilter.getStatuses().isEmpty())
				builder.and(table.status.in(messageFilter.getStatuses()));
			if (messageFilter.getServiceMessage() != null)
			{
				if (Boolean.TRUE.equals(messageFilter.getServiceMessage()))
					builder.and(table.service.eq(EbMSAction.EBMS_SERVICE_URI));
				else
					builder.and(table.service.ne(EbMSAction.EBMS_SERVICE_URI));
			}
			if (messageFilter.getFrom() != null)
				builder.and(table.timeStamp.goe(messageFilter.getFrom().atZone(ZoneId.systemDefault()).toInstant()));
			if (messageFilter.getTo() != null)
				builder.and(table.timeStamp.lt(messageFilter.getTo().atZone(ZoneId.systemDefault()).toInstant()));
		}
		return builder;
	}

	private NumberExpression<Integer> getTimestamp(DateTimePath<Instant> timeStamp, TimeUnit timeUnit)
	{
		switch (timeUnit)
		{
			case HOUR:
				return timeStamp.minute();
			case DAY:
				return timeStamp.hour();
			case MONTH:
				return timeStamp.dayOfMonth();
			case YEAR:
				return timeStamp.month();
			default:
				return null;
		}
	}
}