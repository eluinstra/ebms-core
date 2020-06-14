package nl.clockwork.ebms.event.listener;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class EbMSMessageEventTypeTypeHandler extends BaseTypeHandler<EbMSMessageEventType>
{
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, EbMSMessageEventType parameter, JdbcType jdbcType) throws SQLException
	{
		ps.setInt(i,parameter.getId());
	}

	@Override
	public EbMSMessageEventType getNullableResult(ResultSet rs, String columnName) throws SQLException
	{
		return EbMSMessageEventType.get(rs.getInt(columnName));
	}

	@Override
	public EbMSMessageEventType getNullableResult(ResultSet rs, int columnIndex) throws SQLException
	{
		return EbMSMessageEventType.get(rs.getInt(columnIndex));
	}

	@Override
	public EbMSMessageEventType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
	{
		return EbMSMessageEventType.get(cs.getInt(columnIndex));
	}
}
