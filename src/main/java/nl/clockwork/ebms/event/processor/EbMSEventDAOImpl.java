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
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.transaction.TransactionCallback;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSEventDAOImpl implements EbMSEventDAO
{
	TransactionTemplate transactionTemplate;
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
	public List<EbMSEvent> getEventsBefore(Instant timestamp_, String serverId_) throws DAOException
	{
		val s = select(all)
				.from(ebMSEvent)
				.where(timestamp,isLessThanOrEqualTo(timestamp_))
				.and(serverId,isNull().when(() -> serverId_ == null))
				.and(serverId,isEqualTo(serverId_).when(id -> id != null))
				.orderBy(timestamp)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	@Override
	public List<EbMSEvent> getEventsBefore(Instant timestamp_, String serverId_, int maxNr_) throws DAOException
	{
		val s = select(all)
				.from(ebMSEvent)
				.where(timestamp,isLessThanOrEqualTo(timestamp_))
				.and(serverId,isNull().when(() -> serverId_ == null))
				.and(serverId,isEqualTo(serverId_).when(id -> id != null))
				.orderBy(timestamp)
				.limit(maxNr_)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	@Override
	public void insertEvent(EbMSEvent event, String serverId_) throws DAOException
	{
		val s = insert(event)
				.into(ebMSEvent)
				.map(cpaId).toProperty("cpaId")
				.map(sendDeliveryChannelId).toProperty("sendDeliveryChannelId")
				.map(receiveDeliveryChannelId).toProperty("receiveDeliveryChannelId")
				.map(messageId).toProperty("messageId")
				.map(timeToLive).toProperty("timeToLive")
				.map(timestamp).toProperty("timestamp")
				.map(confidential).toProperty("confidential")
				.map(retries).toProperty("retries")
				.map(serverId).toStringConstant(serverId_)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public void insertEventLog(String messageId_, Instant timestamp_, String uri_, EbMSEventStatus status_, String errorMessage_) throws DAOException
	{
		val s = insert(EbMSEventLog.of(messageId_,timestamp_,uri_,status_,errorMessage_))
				.into(ebMSEventLog)
				.map(messageId).toProperty("messageId")
				.map(timestamp).toProperty("timestamp")
				.map(uri).toProperty("confidential")
				.map(status).toProperty("retries")
				.map(errorMessage).toProperty("errorMessage")
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.insertLog(s);
	}

	@Override
	public void updateEvent(EbMSEvent event) throws DAOException
	{
		val s = update(ebMSEvent)
				.set(timestamp).equalTo(event.getTimestamp())
				.set(retries).equalTo(event.getRetries())
				.where(messageId,isEqualTo(event.getMessageId()))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.update(s);
	}

	@Override
	public void deleteEvent(String messageId_) throws DAOException
	{
		val s = deleteFrom(ebMSEvent)
				.where(messageId,isEqualTo(messageId_))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.delete(s);
	}
}
