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
package nl.clockwork.ebms.event;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.querydsl.model.QEbmsMessage;
import nl.clockwork.ebms.querydsl.model.QMessageEvent;
import nl.clockwork.ebms.service.model.MessageEvent;
import nl.clockwork.ebms.service.model.MessageFilter;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class MessageEventDAOImpl implements MessageEventDAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	QMessageEvent table = QMessageEvent.ebmsMessageEvent;
	QEbmsMessage messageTable = QEbmsMessage.ebmsMessage;

	@Override
	public List<MessageEvent> getEbMSMessageEvents(MessageFilter messageFilter, MessageEventType[] types)
	{
		var whereClause = new BooleanBuilder(messageTable.messageId.eq(table.messageId)
				.and(messageTable.messageNr.eq(0))
				.and(table.processed.eq(false))
				.and(table.eventType.in(types == null ? MessageEventType.values() : types)));
		whereClause = EbMSDAO.applyFilter(messageTable,messageFilter,whereClause);
		return queryFactory.select(Projections.constructor(MessageEvent.class,table.messageId,table.eventType))
				.from(table,messageTable)
				.where(whereClause)
				.orderBy(messageTable.timeStamp.asc())
				.fetch();
	}

	@Override
	public List<MessageEvent> getEbMSMessageEvents(MessageFilter messageFilter, MessageEventType[] types, int maxNr)
	{
		var whereClause = new BooleanBuilder(messageTable.messageId.eq(table.messageId)
				.and(messageTable.messageNr.eq(0))
				.and(table.processed.eq(false))
				.and(table.eventType.in(types == null ? MessageEventType.values() : types)));
		whereClause = EbMSDAO.applyFilter(messageTable,messageFilter,whereClause);
		return queryFactory.select(Projections.constructor(MessageEvent.class,table.messageId,table.eventType))
				.from(table,messageTable)
				.where(whereClause)
				.orderBy(messageTable.timeStamp.asc())
				.limit(maxNr)
				.fetch();
	}

	@Override
	public long insertEbMSMessageEvent(String messageId, MessageEventType eventType)
	{
		return queryFactory.insert(table)
				.set(table.messageId,messageId)
				.set(table.eventType,eventType)
				.set(table.timeStamp,Timestamp.from(Instant.now()))
				.execute();
	}

	@Override
	public long processEbMSMessageEvent(String messageId)
	{
		return queryFactory.update(table)
				.set(table.processed,true)
				.execute();
	}
}
