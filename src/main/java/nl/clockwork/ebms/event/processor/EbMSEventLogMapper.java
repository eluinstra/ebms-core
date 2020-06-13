package nl.clockwork.ebms.event.processor;

import java.sql.JDBCType;
import java.time.Instant;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

@Mapper
public interface EbMSEventLogMapper
{
	public static class EbMSEventLogDSL
	{
		public static final EbMSEventLogTable ebMSEventLog = new EbMSEventLogTable();
		public static final SqlColumn<String> messageId = ebMSEventLog.messageId;
		public static final SqlColumn<Instant> timestamp = ebMSEventLog.timestamp;
		public static final SqlColumn<String> uri = ebMSEventLog.uri;
		public static final SqlColumn<EbMSEventStatus> status = ebMSEventLog.status;
		public static final SqlColumn<String> errorMessage = ebMSEventLog.errorMessage;
		public static final BasicColumn[] all = BasicColumn.columnList(messageId,timestamp,uri,status,errorMessage);

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

	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insertLog(InsertStatementProvider<EbMSEvent> insertStatement);
}
