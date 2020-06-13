package nl.clockwork.ebms.event.processor;

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

@Mapper
public interface EbMSEventMapper
{
	public static class EbMSEventDSL
	{
		public static final EbMSEventTable ebMSEvent = new EbMSEventTable();
		public static final SqlColumn<String> cpaId = ebMSEvent.cpaId;
		public static final SqlColumn<String> sendDeliveryChannelId = ebMSEvent.sendDeliveryChannelId;
		public static final SqlColumn<String> receiveDeliveryChannelId = ebMSEvent.receiveDeliveryChannelId;
		public static final SqlColumn<String> messageId = ebMSEvent.messageId;
		public static final SqlColumn<Instant> timeToLive = ebMSEvent.timeToLive;
		public static final SqlColumn<Instant> timestamp = ebMSEvent.timestamp;
		public static final SqlColumn<Boolean> confidential = ebMSEvent.confidential;
		public static final SqlColumn<Integer> retries = ebMSEvent.retries;
		public static final SqlColumn<String> serverId = ebMSEvent.serverId;
		public static final BasicColumn[] all = BasicColumn.columnList(cpaId,sendDeliveryChannelId,receiveDeliveryChannelId,messageId,timeToLive,timestamp,confidential,retries);

	  public static final class EbMSEventTable extends SqlTable
	  {
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> sendDeliveryChannelId = column("send_channel_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> receiveDeliveryChannelId = column("receive_channel_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Instant> timeToLive = column("time_to_live",JDBCType.TIMESTAMP);
	    public final SqlColumn<Instant> timestamp = column("timestamp",JDBCType.TIMESTAMP);
	    public final SqlColumn<Boolean> confidential = column("is_confidential",JDBCType.BOOLEAN);
	    public final SqlColumn<Integer> retries = column("retries",JDBCType.INTEGER);
	    public final SqlColumn<String> serverId = column("server_id",JDBCType.VARCHAR);

	    public EbMSEventTable()
			{
	    	super("ebms_event");
			}
	  }

		public static final EbMSEventLogTable ebMSEventLog = new EbMSEventLogTable();
		public static final SqlColumn<String> uri = ebMSEventLog.uri;
		public static final SqlColumn<EbMSEventStatus> status = ebMSEventLog.status;
		public static final SqlColumn<String> errorMessage = ebMSEventLog.errorMessage;

		public static final class EbMSEventLogTable extends SqlTable
	  {
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Instant> timestamp = column("time_stamp",JDBCType.TIMESTAMP);
	    public final SqlColumn<String> uri = column("uri",JDBCType.VARCHAR);
	    public final SqlColumn<EbMSEventStatus> status = column("status",JDBCType.SMALLINT,"nl.clockwork.ebms.event.processor.EbMSEventStatusTypeHandler");
	    public final SqlColumn<String> errorMessage = column("error_message",JDBCType.CLOB);

	    public EbMSEventLogTable()
			{
	    	super("ebms_event_log");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="EbMSEventResult", value= {
			@Result(column="cpaId", property="source", jdbcType=JdbcType.VARCHAR),
			@Result(column="sendDeliveryChannelId", property="destination", jdbcType=JdbcType.VARCHAR),
			@Result(column="receiveDeliveryChannelId", property="source", jdbcType=JdbcType.VARCHAR, id=true),
			@Result(column="messageId", property="destination", jdbcType=JdbcType.VARCHAR),
			@Result(column="timeToLive", property="source", jdbcType=JdbcType.VARCHAR, id=true),
			@Result(column="timestamp", property="destination", jdbcType=JdbcType.VARCHAR),
			@Result(column="confidential", property="source", jdbcType=JdbcType.VARCHAR, id=true),
			@Result(column="retries", property="destination", jdbcType=JdbcType.VARCHAR)
	})
	Optional<EbMSEvent> selectOne(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("EbMSEventResult")
	List<EbMSEvent> selectMany(SelectStatementProvider selectStatement);
	
	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<EbMSEvent> insertStatement);

	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insertLog(InsertStatementProvider<EbMSEventLog> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
