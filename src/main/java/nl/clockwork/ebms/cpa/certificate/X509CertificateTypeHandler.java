package nl.clockwork.ebms.cpa.certificate;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import lombok.val;

public class X509CertificateTypeHandler implements TypeHandler<X509Certificate>
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
