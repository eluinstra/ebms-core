package nl.clockwork.ebms.cpa.certificate;

import java.security.cert.X509Certificate;
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
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@Mapper
public interface CertificateMappingMapper
{
	public static class CertificateMappingDSL
	{
		public static final CertificateMappingTable certificateMappingTable = new CertificateMappingTable();
		public static final SqlColumn<String> id = certificateMappingTable.id;
		public static final SqlColumn<X509Certificate> source = certificateMappingTable.source;
		public static final SqlColumn<X509Certificate> destination = certificateMappingTable.destination;
		public static final SqlColumn<String> cpaId = certificateMappingTable.cpaId;

	  public static final class CertificateMappingTable extends SqlTable
	  {
	    public final SqlColumn<String> id = column("id",JDBCType.VARCHAR);
	    public final SqlColumn<X509Certificate> source = column("source",JDBCType.BLOB,"nl.clockwork.ebms.cpa.certificate.X509CertificateTypeHandler");
	    public final SqlColumn<X509Certificate> destination = column("destination",JDBCType.BLOB,"nl.clockwork.ebms.cpa.certificate.X509CertificateTypeHandler");
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.VARCHAR);
	    public final BasicColumn[] all = BasicColumn.columnList(source,destination,cpaId);

	    public CertificateMappingTable()
			{
	    	super("certificate_mapping");
			}
	  }
	}

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	long count(SelectStatementProvider selectStatement);

	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@Results(id="CertificateMappingResult", value= {
			@Result(column="source", property="source", jdbcType=JdbcType.BLOB, typeHandler=X509CertificateTypeHandler.class),
			@Result(column="destination", property="destination", jdbcType=JdbcType.BLOB, typeHandler=X509CertificateTypeHandler.class),
			@Result(column="cpa_id", property="cpaId", jdbcType=JdbcType.VARCHAR)
	})
	Optional<CertificateMapping> selectOne(SelectStatementProvider selectStatement);
	
	@SelectProvider(type=SqlProviderAdapter.class, method="select")
	@ResultMap("CertificateMappingResult")
	List<CertificateMapping> selectMany(SelectStatementProvider selectStatement);
	
	@InsertProvider(type=SqlProviderAdapter.class, method="insert")
	int insert(InsertStatementProvider<CertificateMapping> insertStatement);

	@UpdateProvider(type=SqlProviderAdapter.class, method="update")
	int update(UpdateStatementProvider updateStatement);

	@DeleteProvider(type=SqlProviderAdapter.class, method="delete")
	int delete(DeleteStatementProvider deleteStatement);
}
