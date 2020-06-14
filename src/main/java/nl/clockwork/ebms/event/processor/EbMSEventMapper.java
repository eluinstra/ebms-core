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
		public static final EbMSEventTable ebMSEventTable = new EbMSEventTable();
		public static final SqlColumn<String> cpaId = ebMSEventTable.cpaId;
		public static final SqlColumn<String> sendDeliveryChannelId = ebMSEventTable.sendDeliveryChannelId;
		public static final SqlColumn<String> receiveDeliveryChannelId = ebMSEventTable.receiveDeliveryChannelId;
		public static final SqlColumn<String> messageId = ebMSEventTable.messageId;
		public static final SqlColumn<Instant> timeToLive = ebMSEventTable.timeToLive;
		public static final SqlColumn<Instant> timestamp = ebMSEventTable.timestamp;
		public static final SqlColumn<Boolean> confidential = ebMSEventTable.confidential;
		public static final SqlColumn<Integer> retries = ebMSEventTable.retries;
		public static final SqlColumn<String> serverId = ebMSEventTable.serverId;

	  public static final class EbMSEventTable extends SqlTable
	  {
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> sendDeliveryChannelId = column("send_channel_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> receiveDeliveryChannelId = column("receive_channel_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Instant> timeToLive = column("time_to_live",JDBCType.TIMESTAMP);
	    public final SqlColumn<Instant> timestamp = column("timestamp",JDBCType.TIMESTAMP);
	    public final SqlColumn<Boolean> confidential = column("is_confidential",JDBCType.BOOLEAN);
	    public final SqlColumn<Integer> retries = column("retries",JDBCType.SMALLINT);
	    public final SqlColumn<String> serverId = column("server_id",JDBCType.VARCHAR);
			public final BasicColumn[] all = BasicColumn.columnList(cpaId,sendDeliveryChannelId,receiveDeliveryChannelId,messageId,timeToLive,timestamp,confidential,retries);

	    public EbMSEventTable()
			{
	    	super("ebms_event");
			}
	  }

		public static final EbMSEventLogTable ebMSEventLog = new EbMSEventLogTable();

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
			@Result(column="receiveDeliveryChannelId", property="source", jdbcType=JdbcType.VARCHAR),
			@Result(column="messageId", property="destination", jdbcType=JdbcType.VARCHAR),
			@Result(column="timeToLive", property="source", jdbcType=JdbcType.TIMESTAMP),
			@Result(column="timestamp", property="destination", jdbcType=JdbcType.TIMESTAMP),
			@Result(column="confidential", property="source", jdbcType=JdbcType.VARCHAR),
			@Result(column="retries", property="destination", jdbcType=JdbcType.SMALLINT)
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
