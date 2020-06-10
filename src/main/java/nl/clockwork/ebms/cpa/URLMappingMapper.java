package nl.clockwork.ebms.cpa;

import java.sql.JDBCType;
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
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import nl.clockwork.ebms.service.cpa.url.URLMapping;

@Mapper
public interface URLMappingMapper
{
	public static class URLMappingDSL
	{
		public static final URLMapping urlMapping = new URLMapping();
		public static final SqlColumn<String> source = urlMapping.source;
		public static final SqlColumn<String> destination = urlMapping.destination;

	  public static final class URLMapping extends SqlTable
	  {
	    public final SqlColumn<String> source = column("source",JDBCType.VARCHAR);
	    public final SqlColumn<String> destination = column("destination",JDBCType.VARCHAR);

	    public URLMapping()
			{
	    	super("url_mapping");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("URLMappingResult")
	Optional<URLMapping> selectOne(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="URLMappingResult", value= {
			@Result(column="source", property="source", jdbcType=JdbcType.VARCHAR, id=true),
			@Result(column="destination", property="destination", jdbcType=JdbcType.VARCHAR)
	})
	List<URLMapping> selectMany(SelectStatementProvider selectStatement);
	
	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<URLMapping> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
