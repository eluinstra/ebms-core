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
package nl.clockwork.ebms.task;

import java.time.Instant;
import java.util.List;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
class SendTaskDAOImpl implements SendTaskDAO
{
	@NonNull
	SQLQueryFactory queryFactory;
	QSendTask table = QSendTask.sendTask;
	QSendLog logTable = QSendLog.sendLog;
	Expression<SendTask> createSendTaskProjection = Projections.constructor(
			SendTask.class,table.cpaId,table.sendChannelId,table.receiveChannelId,table.messageId,table.timeToLive,table.timeStamp,table.isConfidential,table.retries);

	@Override
	public List<SendTask> getTasksBefore(Instant timestamp, String serverId)
	{
		return queryFactory.select(createSendTaskProjection)
				.from(table)
				.where(table.timeStamp.loe(timestamp)
						.and(serverId == null ? table.serverId.isNull() : table.serverId.eq(serverId)))
				.orderBy(table.timeStamp.asc())
				.fetch();
	}

	@Override
	public List<SendTask> getTasksBefore(Instant timestamp, String serverId, int maxNr)
	{
		return queryFactory.select(createSendTaskProjection)
				.from(table)
				.where(table.timeStamp.loe(timestamp)
						.and(serverId == null ? table.serverId.isNull() : table.serverId.eq(serverId)))
				.orderBy(table.timeStamp.asc())
				.limit(maxNr)
				.fetch();
	}
	
	@Override
	public long insertTask(SendTask task, String serverId)
	{
		return queryFactory.insert(table)
				.set(table.cpaId,task.getCpaId())
				.set(table.sendChannelId,task.getSendDeliveryChannelId())
				.set(table.receiveChannelId,task.getReceiveDeliveryChannelId())
				.set(table.messageId,task.getMessageId())
				.set(table.timeToLive,task.getTimeToLive())
				.set(table.timeStamp,task.getTimestamp())
				.set(table.isConfidential,task.isConfidential())
				.set(table.retries,task.getRetries())
				.set(table.serverId,serverId)
				.execute();
	}

	@Override
	public long updateTask(SendTask task)
	{
		return queryFactory.update(table)
				.set(table.timeStamp,task.getTimestamp())
				.set(table.retries,task.getRetries())
				.where(table.messageId.eq(task.getMessageId()))
				.execute();
	}
	
	@Override
	public long deleteTask(String messageId)
	{
		return queryFactory.delete(table)
				.where(table.messageId.eq(messageId))
				.execute();
	}

	@Override
	public long insertLog(String messageId, Instant timestamp, String uri, SendTaskStatus status, String errorMessage)
	{
		return queryFactory.insert(logTable)
				.set(logTable.messageId,messageId)
				.set(logTable.timeStamp,timestamp)
				.set(logTable.uri,uri)
				.set(logTable.status,status)
				.set(logTable.errorMessage,errorMessage)
				.execute();
	}
}
