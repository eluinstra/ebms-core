package nl.clockwork.ebms.cpa;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.CallableStatement;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.dynamic.sql.SqlColumn;
import org.mybatis.dynamic.sql.SqlTable;
import org.mybatis.dynamic.sql.delete.render.DeleteStatementProvider;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.render.UpdateStatementProvider;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;

import lombok.val;
import nl.clockwork.ebms.service.cpa.certificate.CertificateMapping;

@Mapper
public interface CertificateMappingMapper
{
	public static class CertificateMappingDSL
	{
		public static final CertificateMappingTable certificateMapping = new CertificateMappingTable();
		public static final SqlColumn<String> id = certificateMapping.id;
		public static final SqlColumn<X509Certificate> source = certificateMapping.source;
		public static final SqlColumn<X509Certificate> destination = certificateMapping.destination;
		public static final SqlColumn<String> cpaId = certificateMapping.cpaId;

	  public static final class CertificateMappingTable extends SqlTable
	  {
	    public final SqlColumn<String> id = column("id",JDBCType.VARCHAR);
	    public final SqlColumn<X509Certificate> source = column("source",JDBCType.BLOB);
	    public final SqlColumn<X509Certificate> destination = column("destination",JDBCType.BLOB);
	    public final SqlColumn<String> cpaId = column("cpa_id",JDBCType.BLOB);

	    public CertificateMappingTable()
			{
	    	super("certificate_mapping");
			}
	  }
	}

	public static class X509CertificateTypeHandler implements TypeHandler<X509Certificate>
	{
		@Override
		public void setParameter(PreparedStatement ps, int i, X509Certificate parameter, JdbcType jdbcType) throws SQLException
		{
			try
			{
				ps.setBytes(i,parameter.getEncoded());
			}
			catch (CertificateEncodingException e)
			{
				throw new SQLException(e);
			}
		}

		@Override
		public X509Certificate getResult(ResultSet rs, String columnName) throws SQLException
		{
			try
			{
				val certificateFactory = CertificateFactory.getInstance("X509");
				return (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream(columnName));
			}
			catch (CertificateException e)
			{
				throw new SQLException(e);
			}
		}

		@Override
		public X509Certificate getResult(ResultSet rs, int columnIndex) throws SQLException
		{
			try
			{
				val certificateFactory = CertificateFactory.getInstance("X509");
				return (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream(columnIndex));
			}
			catch (CertificateException e)
			{
				throw new SQLException(e);
			}
		}

		@Override
		public X509Certificate getResult(CallableStatement cs, int columnIndex) throws SQLException
		{
			try
			{
				val certificateFactory = CertificateFactory.getInstance("X509");
				return (X509Certificate)certificateFactory.generateCertificate(new ByteArrayInputStream(cs.getBytes(columnIndex)));
			}
			catch (CertificateException e)
			{
				throw new SQLException(e);
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
