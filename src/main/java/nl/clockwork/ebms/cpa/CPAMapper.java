package nl.clockwork.ebms.cpa;

import java.sql.JDBCType;
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
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

@Mapper
public interface CPAMapper
{
	public static class CPADSL
	{
		public static final cpaTable cpaTable = new cpaTable();
		public static final SqlColumn<String> cpaId = cpaTable.cpaId;
		public static final SqlColumn<CollaborationProtocolAgreement> cpa = cpaTable.cpa;

	  public static final class cpaTable extends SqlTable
	  {
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.VARCHAR);
	    public final SqlColumn<CollaborationProtocolAgreement> cpa = column("cpa",JDBCType.CLOB,"nl.clockwork.ebms.cpa.CollaborationProtocolAgreementTypeHandler");

	    public cpaTable()
			{
	    	super("cpa");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="CPAMappingResult")
	@ConstructorArgs({
		@Arg(column = "cpa", javaType = CollaborationProtocolAgreement.class, jdbcType=JdbcType.VARCHAR, typeHandler = CollaborationProtocolAgreementTypeHandler.class)
	})
	Optional<CPA> selectOne(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("CPAMappingResult")
	List<String> selectMany(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="CPAIds", value= {
			@Result(column="cpa_id", property="cpaId", jdbcType=JdbcType.VARCHAR, id=true),
	})
	List<String> selectCpaIds(SelectStatementProvider selectStatement);
	
	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<CPA> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
