package nl.clockwork.ebms.event.listener;

import static nl.clockwork.ebms.dao.EbMSMessageMapper.EbMSMessageDSL.ebMSMessageTable;
import static nl.clockwork.ebms.event.listener.EbMSMessageEventMapper.EbMSMessageEventDSL.all;
import static nl.clockwork.ebms.event.listener.EbMSMessageEventMapper.EbMSMessageEventDSL.ebMSMessageEventTable;
import static nl.clockwork.ebms.event.listener.EbMSMessageEventMapper.EbMSMessageEventDSL.eventType;
import static nl.clockwork.ebms.event.listener.EbMSMessageEventMapper.EbMSMessageEventDSL.processed;
import static nl.clockwork.ebms.event.listener.EbMSMessageEventMapper.EbMSMessageEventDSL.timestamp;
import static org.mybatis.dynamic.sql.SqlBuilder.equalTo;
import static org.mybatis.dynamic.sql.SqlBuilder.insert;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;
import static org.mybatis.dynamic.sql.SqlBuilder.isIn;
import static org.mybatis.dynamic.sql.SqlBuilder.select;
import static org.mybatis.dynamic.sql.SqlBuilder.update;

import java.util.List;

import org.mybatis.dynamic.sql.render.RenderingStrategies;
import org.mybatis.dynamic.sql.select.QueryExpressionDSL;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.service.model.EbMSMessageContext;
import nl.clockwork.ebms.service.model.EbMSMessageEvent;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public class EbMSMessageEventDAOImpl implements EbMSMessageEventDAO
{
	@NonNull
	EbMSMessageEventMapper mapper;

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types) throws DAOException
	{
		var b = select(all)
				.from(ebMSMessageEventTable)
				.join(ebMSMessageTable).on(ebMSMessageEventTable.messageId,equalTo(ebMSMessageTable.messageId))
				.where(processed,isEqualTo(false))
				.and(eventType,isIn(types))
				.and(ebMSMessageTable.messageNr,isEqualTo(0));
		applyMessageContextFilter(b,messageContext);
		val s = b.orderBy(timestamp)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	@Override
	public List<EbMSMessageEvent> getEbMSMessageEvents(EbMSMessageContext messageContext, EbMSMessageEventType[] types, int maxNr) throws DAOException
	{
		var b = select(all)
				.from(ebMSMessageEventTable)
				.join(ebMSMessageTable).on(ebMSMessageEventTable.messageId,equalTo(ebMSMessageTable.messageId))
				.where(processed,isEqualTo(false))
				.and(eventType,isIn(types))
				.and(ebMSMessageTable.messageNr,isEqualTo(0));
		applyMessageContextFilter(b,messageContext);
		val s = b.orderBy(timestamp)
				.limit(maxNr)
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.selectMany(s);
	}

	public void applyMessageContextFilter(QueryExpressionDSL<org.mybatis.dynamic.sql.select.SelectModel>.QueryExpressionWhereBuilder s, EbMSMessageContext messageContext)
	{
		if (messageContext != null)
		{
			s.and(ebMSMessageTable.cpaId,isEqualTo(messageContext.getCpaId()).when(v -> v != null));
			if (messageContext.getFromParty() != null)
			{
				s.and(ebMSMessageTable.fromPartyId,isEqualTo(messageContext.getFromParty().getPartyId()).when(v -> v != null));
				s.and(ebMSMessageTable.fromRole,isEqualTo(messageContext.getFromParty().getPartyId()).when(v -> v != null));
			}
			if (messageContext.getToParty() != null)
			{
				s.and(ebMSMessageTable.toPartyId,isEqualTo(messageContext.getToParty().getPartyId()).when(v -> v != null));
				s.and(ebMSMessageTable.toRole,isEqualTo(messageContext.getToParty().getPartyId()).when(v -> v != null));
			}
			s.and(ebMSMessageTable.service,isEqualTo(messageContext.getService()).when(v -> v != null));
			s.and(ebMSMessageTable.action,isEqualTo(messageContext.getAction()).when(v -> v != null));
			s.and(ebMSMessageTable.conversationId,isEqualTo(messageContext.getConversationId()).when(v -> v != null));
			s.and(ebMSMessageTable.messageId,isEqualTo(messageContext.getMessageId()).when(v -> v != null));
			s.and(ebMSMessageTable.refToMessageId,isEqualTo(messageContext.getRefToMessageId()).when(v -> v != null));
			s.and(ebMSMessageTable.status,isEqualTo(messageContext.getMessageStatus()).when(v -> v != null));
		}
	}

	@Override
	public void insertEbMSMessageEvent(EbMSMessageEvent event) throws DAOException
	{
		val s = insert(event)
				.into(ebMSMessageEventTable)
				.map(ebMSMessageEventTable.messageId).toProperty("messageId")
				.map(ebMSMessageEventTable.eventType).toProperty("type")
				.map(timestamp).toProperty("timestamp")
				.build()
				.render(RenderingStrategies.MYBATIS3);
		mapper.insert(s);
	}

	@Override
	public int processEbMSMessageEvent(String messageId) throws DAOException
	{
		val s = update(ebMSMessageEventTable)
				.set(processed).equalTo(true)
				.where(ebMSMessageEventTable.messageId,isEqualTo(messageId))
				.build()
				.render(RenderingStrategies.MYBATIS3);
		return mapper.update(s);
	}
}
