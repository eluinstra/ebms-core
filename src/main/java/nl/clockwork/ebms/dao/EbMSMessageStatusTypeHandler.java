package nl.clockwork.ebms.dao;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import nl.clockwork.ebms.EbMSMessageStatus;

public class EbMSMessageStatusTypeHandler extends BaseTypeHandler<EbMSMessageStatus>
{
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, EbMSMessageStatus parameter, JdbcType jdbcType) throws SQLException
	{
		ps.setInt(i,parameter.getId());
	}

	@Override
	public EbMSMessageStatus getNullableResult(ResultSet rs, String columnName) throws SQLException
	{
		return EbMSMessageStatus.get(rs.getInt(columnName));
	}

	@Override
	public EbMSMessageStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException
	{
		return EbMSMessageStatus.get(rs.getInt(columnIndex));
	}

	@Override
	public EbMSMessageStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
	{
		return EbMSMessageStatus.get(cs.getInt(columnIndex));
	}
}
