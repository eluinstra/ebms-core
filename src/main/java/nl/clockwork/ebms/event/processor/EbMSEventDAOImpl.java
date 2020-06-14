package nl.clockwork.ebms.event.processor;

import static nl.clockwork.ebms.event.processor.EbMSEventMapper.EbMSEventDSL.*;
import static org.mybatis.dynamic.sql.SqlBuilder.*;

import java.time.Instant;
import java.util.List;

import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.transaction.TransactionCallback;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSEventDAOImpl implements EbMSEventDAO
{
	@NonNull
	TransactionTemplate transactionTemplate;
	@NonNull
	EbMSEventMapper mapper;

	@Override
	public void executeTransaction(TransactionCallback callback) throws DAOException
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

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId) throws DAOException
	{
		val s = select(ebMSEventTable.all)
				.from(ebMSEventTable)
				.where(ebMSEventTable.timestamp,isLessThanOrEqualTo(timestamp))
				.and(ebMSEventTable.serverId,isNull().when(() -> serverId == null))
				.and(ebMSEventTable.serverId,isEqualTo(serverId).when(id -> id != null))
				.orderBy(ebMSEventTable.timestamp)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp, String serverId, int maxNr) throws DAOException
	{
		val s = select(ebMSEventTable.all)
				.from(ebMSEventTable)
				.where(ebMSEventTable.timestamp,isLessThanOrEqualTo(timestamp))
				.and(ebMSEventTable.serverId,isNull().when(() -> serverId == null))
				.and(ebMSEventTable.serverId,isEqualTo(serverId).when(id -> id != null))
				.orderBy(ebMSEventTable.timestamp)
				.limit(maxNr)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	@Override
	public void insertEvent(EbMSEvent event, String serverId) throws DAOException
	{
		val s = insert(event)
				.into(ebMSEventTable)
				.map(cpaId).toProperty("cpaId")
				.map(sendDeliveryChannelId).toProperty("sendDeliveryChannelId")
				.map(receiveDeliveryChannelId).toProperty("receiveDeliveryChannelId")
				.map(messageId).toProperty("messageId")
				.map(timeToLive).toProperty("timeToLive")
				.map(timestamp).toProperty("timestamp")
				.map(confidential).toProperty("confidential")
				.map(retries).toProperty("retries")
				.map(ebMSEventTable.serverId).toStringConstant(serverId)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public void insertEventLog(String messageId, Instant timestamp, String uri, EbMSEventStatus status, String errorMessage) throws DAOException
	{
		val s = insert(EbMSEventLog.of(messageId,timestamp,uri,status,errorMessage))
				.into(ebMSEventLog)
				.map(ebMSEventLog.messageId).toProperty("messageId")
				.map(ebMSEventLog.timestamp).toProperty("timestamp")
				.map(ebMSEventLog.uri).toProperty("confidential")
				.map(ebMSEventLog.status).toProperty("retries")
				.map(ebMSEventLog.errorMessage).toProperty("errorMessage")
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.insertLog(s);
	}

	@Override
	public void updateEvent(EbMSEvent event) throws DAOException
	{
		val s = update(ebMSEventTable)
				.set(timestamp).equalTo(event.getTimestamp())
				.set(retries).equalTo(event.getRetries())
				.where(messageId,isEqualTo(event.getMessageId()))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.update(s);
	}

	@Override
	public void deleteEvent(String messageId) throws DAOException
	{
		val s = deleteFrom(ebMSEventTable)
				.where(ebMSEventTable.messageId,isEqualTo(messageId))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.delete(s);
	}
}
