package nl.clockwork.ebms.cpa;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.bind.JAXBException;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import nl.clockwork.ebms.jaxb.JAXBParser;

public class CollaborationProtocolAgreementTypeHandler implements TypeHandler<CollaborationProtocolAgreement>
{

	@Override
	public void setParameter(PreparedStatement ps, int i, CollaborationProtocolAgreement parameter, JdbcType jdbcType) throws SQLException
	{
		try
		{
			ps.setString(i,JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(parameter));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public CollaborationProtocolAgreement getResult(ResultSet rs, String columnName) throws SQLException
	{
		try
		{
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(rs.getString(columnName));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public CollaborationProtocolAgreement getResult(ResultSet rs, int columnIndex) throws SQLException
	{
		try
		{
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(rs.getString(columnIndex));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public CollaborationProtocolAgreement getResult(CallableStatement cs, int columnIndex) throws SQLException
	{
		try
		{
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(cs.getString(columnIndex));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}

}
