package nl.clockwork.ebms.dao;

import java.io.InputStream;
import java.sql.JDBCType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
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

import nl.clockwork.ebms.EbMSMessageStatus;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.service.model.EbMSMessageContext;

@Mapper
public interface EbMSMessageMapper
{
	public static class EbMSMessageDSL
	{
		public static final EbMSMessageTable ebMSMessageTable = new EbMSMessageTable();

	  public static final class EbMSMessageTable extends SqlTable
	  {
	    public final SqlColumn<Instant> timestamp = column("time_stamp",JDBCType.TIMESTAMP);
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> conversationId = column("conversation_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Integer> messageNr = column("message_nr",JDBCType.SMALLINT);
	    public final SqlColumn<String> refToMessageId = column("ref_to_message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Instant> timeToLive = column("time_to_live",JDBCType.TIMESTAMP);
	    public final SqlColumn<Instant> persistTime = column("persist_time",JDBCType.TIMESTAMP);
	    public final SqlColumn<String> fromPartyId = column("from_party_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> fromRole = column("from_role",JDBCType.VARCHAR);
	    public final SqlColumn<String> toPartyId = column("to_party_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> toRole = column("to_role",JDBCType.VARCHAR);
	    public final SqlColumn<String> service = column("service",JDBCType.VARCHAR);
	    public final SqlColumn<String> action = column("action",JDBCType.VARCHAR);
	    public final SqlColumn<String> content = column("content",JDBCType.CLOB);
	    public final SqlColumn<EbMSMessageStatus> status = column("status",JDBCType.SMALLINT,"nl.clockwork.ebms.dao.EbMSMessageStatusTypeHandler");
	    public final SqlColumn<Instant> statusTime = column("status_time",JDBCType.TIMESTAMP);
	    public final BasicColumn[] all = BasicColumn.columnList(timestamp,cpaId,conversationId,messageId,messageNr,refToMessageId,timestamp,persistTime,fromPartyId,fromRole,toPartyId,toRole,service,action,content,status,statusTime);
	    public final BasicColumn[] messageContext = BasicColumn.columnList(cpaId,fromPartyId,fromRole,toPartyId,toRole,service,action,timestamp,conversationId,messageId,refToMessageId,status);

	    public EbMSMessageTable()
			{
	    	super("ebms_message");
			}
	  }

		public static final EbMSAttachmentTable ebMSAttachmentTable = new EbMSAttachmentTable();

		public static final class EbMSAttachmentTable extends SqlTable
	  {
	    public final SqlColumn<String> messageId = column("message_id",JDBCType.VARCHAR);
	    public final SqlColumn<Integer> messageNr = column("message_nr",JDBCType.SMALLINT);
	    public final SqlColumn<Integer> orderNr = column("order_nr",JDBCType.SMALLINT);
	    public final SqlColumn<String> name = column("name",JDBCType.VARCHAR);
	    public final SqlColumn<String> contentId = column("content_id",JDBCType.VARCHAR);
	    public final SqlColumn<String> contentType = column("content_type",JDBCType.VARCHAR);
	    public final SqlColumn<InputStream> content = column("content",JDBCType.BLOB);
	  	
	    public EbMSAttachmentTable()
			{
	    	super("ebms_attachment");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="EbMSMessageResult", value= {
			@Result(column="cpa_id", property="cpaId", jdbcType=JdbcType.VARCHAR),
			@Result(column="conversation_id", property="conversationId", jdbcType=JdbcType.VARCHAR),
			@Result(column="message_id", property="messageId", jdbcType=JdbcType.VARCHAR),
			@Result(column="message_nr", property="destination", jdbcType=JdbcType.SMALLINT),
			@Result(column="ref_to_message_id", property="refToMessageId", jdbcType=JdbcType.VARCHAR),
			@Result(column="time_to_live", property="timeToLive", jdbcType=JdbcType.TIMESTAMP),
			@Result(column="persist_time", property="persistTime", jdbcType=JdbcType.TIMESTAMP),
			@Result(column="from_party_id", property="fromPartyId", jdbcType=JdbcType.VARCHAR),
			@Result(column="from_role", property="fromRole", jdbcType=JdbcType.VARCHAR),
			@Result(column="to_party_id", property="toPartyId", jdbcType=JdbcType.VARCHAR),
			@Result(column="to_role", property="toRole", jdbcType=JdbcType.VARCHAR),
			@Result(column="service", property="service", jdbcType=JdbcType.VARCHAR),
			@Result(column="action", property="action", jdbcType=JdbcType.VARCHAR),
			@Result(column="content", property="content", jdbcType=JdbcType.CLOB),
			@Result(column="status", property="status", jdbcType=JdbcType.SMALLINT, typeHandler = EbMSMessageStatusTypeHandler.class),
			@Result(column="status_time", property="statusTime", jdbcType=JdbcType.TIMESTAMP)
	})
	Optional<EbMSMessage> selectOne(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ConstructorArgs(value = {
			@Arg(column="cpa_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="from_party_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="from_role", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="to_party_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="to_role", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="service", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="action", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="time_stamp", javaType = Instant.class, jdbcType=JdbcType.TIMESTAMP),
			@Arg(column="conversation_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="message_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="ref_to_message_id", javaType = String.class, jdbcType=JdbcType.VARCHAR),
			@Arg(column="status", javaType = EbMSMessageStatus.class, jdbcType=JdbcType.SMALLINT, typeHandler = EbMSMessageStatusTypeHandler.class)
	})
	Optional<EbMSMessageContext> selectMessageContext(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="Content", value= {
			//@Result(column="content", jdbcType=JdbcType.CLOB, typeHandler = DocumentTypeHandler.class)
			@Result(column="content", jdbcType=JdbcType.CLOB)
	})
	//Optional<Document> selectContent(SelectStatementProvider selectStatement);
	Optional<String> selectContent(SelectStatementProvider selectStatement);

//	@Select("select content from ebms_message where message_id = #{messageId} and message_nr = 0 and (status is null or status = #{EbMSMessageStatusId})")
//	@Results(id="EbMSDocument", value= {
//			@Result(column="content", property="content", jdbcType=JdbcType.CLOB, typeHandler = DocumentTypeHandler.class),
//			@Result(column="content", property="content", jdbcType=JdbcType.CLOB, typeHandler = DocumentTypeHandler.class),
//			@Result(column="content", property="content", jdbcType=JdbcType.CLOB, typeHandler = DocumentTypeHandler.class, many = @Many(select = "selectAttachments"))
//	})
//	Optional<EbMSDocument> selectEbMSDocumentIfUnsent(@Param("messageId") String messageId, @Param("ebMSMessageStatusId") int ebMSMessageStatusId);
	
//	List<EbMSAttachment> selectAttachments(String messageId, int messageNr);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("EbMSMessageResult")
	List<EbMSMessage> selectMany(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="MessageIds", value= {
			@Result(column="message_id", jdbcType=JdbcType.VARCHAR),
	})
	List<String> selectMessageIds(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="MessageStatus", value= {
			@Result(column="status", jdbcType=JdbcType.SMALLINT, typeHandler = EbMSMessageStatusTypeHandler.class)
	})
	Optional<EbMSMessageStatus> selectMessageStatus(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="MessageAction", value= {
			@Result(column="action", jdbcType=JdbcType.VARCHAR)
	})
	Optional<String> selectMessageAction(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="PersistTime", value= {
			@Result(column="persist_time", jdbcType=JdbcType.TIMESTAMP),
	})
	Optional<Instant> selectPersistTime(SelectStatementProvider selectStatement);

	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<EbMSMessage> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
