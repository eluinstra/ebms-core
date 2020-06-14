package nl.clockwork.ebms.event.listener;

import java.sql.JDBCType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import nl.clockwork.ebms.service.model.EbMSMessageEvent;

@Mapper
public interface EbMSMessageEventMapper
{
	public static class EbMSMessageEventDSL
	{
		public static final EbMSMessageEventTable ebMSMessageEventTable = new EbMSMessageEventTable();
		public static final SqlColumn<String> messageId = ebMSMessageEventTable.messageId;
		public static final SqlColumn<EbMSMessageEventType> eventType = ebMSMessageEventTable.eventType;
		public static final SqlColumn<Instant> timestamp = ebMSMessageEventTable.timestamp;
		public static final SqlColumn<Boolean> processed = ebMSMessageEventTable.processed;
		public static final BasicColumn[] all = BasicColumn.columnList(messageId,eventType);

	  public static final class EbMSMessageEventTable extends SqlTable
	  {
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<EbMSMessageEventType> eventType = column("event_type",JDBCType.SMALLINT,"nl.clockwork.ebms.event.listener.EbMSMessageEventTypeTypeHandler");
	    public final SqlColumn<Instant> timestamp = column("time_stamp",JDBCType.TIMESTAMP);
	    public final SqlColumn<Boolean> processed = column("processed",JDBCType.BOOLEAN);

	    public EbMSMessageEventTable()
			{
	    	super("ebms_message_event");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="EbMSMessageEventResult", value= {
			@Result(column="message_id", property="messageId", jdbcType=JdbcType.VARCHAR, id=true),
			@Result(column="event_type", property="eventType", jdbcType=JdbcType.SMALLINT, typeHandler = EbMSMessageEventTypeTypeHandler.class)
	})
	Optional<EbMSMessageEvent> selectOne(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("EbMSMessageEventResult")
	List<EbMSMessageEvent> selectMany(SelectStatementProvider selectStatement);
	
	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<EbMSMessageEvent> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
