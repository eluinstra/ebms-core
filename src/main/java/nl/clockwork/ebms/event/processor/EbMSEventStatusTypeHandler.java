package nl.clockwork.ebms.event.processor;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

public class EbMSEventStatusTypeHandler extends BaseTypeHandler<EbMSEventStatus>
{
	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, EbMSEventStatus parameter, JdbcType jdbcType) throws SQLException
	{
		ps.setInt(i,parameter.getId());
	}

	@Override
	public EbMSEventStatus getNullableResult(ResultSet rs, String columnName) throws SQLException
	{
		return EbMSEventStatus.get(rs.getInt(columnName));
	}

	@Override
	public EbMSEventStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException
	{
		return EbMSEventStatus.get(rs.getInt(columnIndex));
	}

	@Override
	public EbMSEventStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
	{
		return EbMSEventStatus.get(cs.getInt(columnIndex));
	}
}
