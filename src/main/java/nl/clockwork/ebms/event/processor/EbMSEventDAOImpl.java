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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.Action;
import nl.clockwork.ebms.querydsl.model.QEbmsEvent;
import nl.clockwork.ebms.querydsl.model.QEbmsEventLog;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSEventDAOImpl implements EbMSEventDAO
{
	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	SQLQueryFactory queryFactory;
	QEbmsEvent table = QEbmsEvent.ebmsEvent;
	QEbmsEventLog logTable = QEbmsEventLog.ebmsEventLog;

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public void executeTransaction(final Action action)
	{
		action.run();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId)
	{
		return queryFactory.select(createEbMSEventProjection())
				.from(table)
				.where(table.timeStamp.loe(timestamp)
						.and(serverId == null ? table.serverId.isNull() : table.serverId.eq(serverId)))
				.orderBy(table.timeStamp.asc())
				.fetch();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId, int maxNr)
	{
		return queryFactory.select(createEbMSEventProjection())
				.from(table)
				.where(table.timeStamp.loe(timestamp)
						.and(serverId == null ? table.serverId.isNull() : table.serverId.eq(serverId)))
				.orderBy(table.timeStamp.asc())
				.limit(maxNr)
				.fetch();
	}
	
	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long insertEvent(EbMSEvent event, String serverId)
	{
		return queryFactory.insert(table)
				.set(table.cpaId,event.getCpaId())
				.set(table.sendChannelId,event.getSendDeliveryChannelId())
				.set(table.receiveChannelId,event.getReceiveDeliveryChannelId())
				.set(table.messageId,event.getMessageId())
				.set(table.timeToLive,event.getTimeToLive())
				.set(table.timeStamp,event.getTimestamp())
				.set(table.isConfidential,event.isConfidential())
				.set(table.retries,event.getRetries())
				.set(table.serverId,serverId)
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long updateEvent(EbMSEvent event)
	{
		return queryFactory.update(table)
				.set(table.timeStamp,event.getTimestamp())
				.set(table.retries,event.getRetries())
				.where(table.messageId.eq(event.getMessageId()))
				.execute();
	}
	
	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long deleteEvent(String messageId)
	{
		return queryFactory.delete(table)
				.where(table.messageId.eq(messageId))
				.execute();
	}

	@Override
	@Transactional(transactionManager = "dataSourceTransactionManager")
	public long insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage)
	{
		return queryFactory.insert(logTable)
				.set(logTable.messageId,messageId)
				.set(logTable.timeStamp,timestamp)
				.set(logTable.uri,uri)
				.set(logTable.status,status)
				.set(logTable.errorMessage,errorMessage)
				.execute();
	}

	private ConstructorExpression<EbMSEvent> createEbMSEventProjection()
	{
		return Projections.constructor(
				EbMSEvent.class,table.cpaId,table.sendChannelId,table.receiveChannelId,table.messageId,table.timeToLive,table.timeStamp,table.isConfidential,table.retries);
	}
}
