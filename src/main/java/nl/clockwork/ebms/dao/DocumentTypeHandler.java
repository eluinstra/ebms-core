package nl.clockwork.ebms.dao;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.util.DOMUtils;

public class DocumentTypeHandler extends BaseTypeHandler<Document>
{
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Document parameter, JdbcType jdbcType) throws SQLException
	{
		try
		{
			ps.setString(i,DOMUtils.toString(parameter));
		}
		catch (TransformerException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public Document getNullableResult(ResultSet rs, String columnName) throws SQLException
	{
		try
		{
			return DOMUtils.read(rs.getString(columnName));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public Document getNullableResult(ResultSet rs, int columnIndex) throws SQLException
	{
		try
		{
			return DOMUtils.read(rs.getString(columnIndex));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public Document getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
	{
		try
		{
			return DOMUtils.read(cs.getString(columnIndex));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new SQLException(e);
		}
	}
}
