package nl.clockwork.ebms.event.listener;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.querydsl.model.QEbmsMessageEvent;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageEventDAOImpl implements EbMSMessageEventDAO
{
	@NonNull
	JdbcTemplate jdbcTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QEbmsMessageEvent table = QEbmsMessageEvent.ebmsMessageEvent;
	QEbmsMessage messageTable = QEbmsMessage.ebmsMessage;

	RowMapper<EbMSMessageEvent> ebMSMessageEventRowMapper = (rs,rowNum) ->
	{
		return new EbMSMessageEvent(rs.getString("message_id"),EbMSMessageEventType.values()[rs.getInt("event_type")]);
	};

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types)
	{
		val query = queryFactory.select(table.messageId,table.eventType)
				.from(table,messageTable)
				.where(messageTable.messageId.eq(messageContext.getMessageId())
						.and(messageTable.messageNr.eq(0))
						.and(table.processed.eq(false))
						.and(table.eventType.in(types == null ? EbMSMessageEventType.values() : types))
						//to filter
						)
				.orderBy(messageTable.timeStamp.asc())
				.getSQL();
		return jdbcTemplate.query(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				ebMSMessageEventRowMapper);
	}

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr)
	{
		val query = queryFactory.select(table.messageId,table.eventType)
				.from(table,messageTable)
				.where(messageTable.messageId.eq(messageContext.getMessageId())
						.and(messageTable.messageNr.eq(0))
						.and(table.processed.eq(false))
						.and(table.eventType.in(types == null ? EbMSMessageEventType.values() : types))
						//to filter
						)
				.orderBy(messageTable.timeStamp.asc())
				.limit(maxNr)
				.getSQL();
		return jdbcTemplate.query(
				query.getSQL(),
				query.getNullFriendlyBindings().toArray(),
				ebMSMessageEventRowMapper);
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long insertEbMSMessageEvent(String messageId, EbMSMessageEventType eventType)
	{
		return queryFactory.insert(table)
				.set(table.messageId,messageId)
				.set(table.eventType,eventType)
				.set(table.timeStamp,Timestamp.from(Instant.now()))
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long processEbMSMessageEvent(String messageId)
	{
		return queryFactory.update(table)
				.set(table.processed,true)
				.execute();
	}
}
