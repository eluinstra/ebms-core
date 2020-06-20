package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.querydsl.sql.types.AbstractType;

import nl.clockwork.ebms.event.processor.EbMSEventStatus;

public class EbMSEventStatusType extends AbstractType<EbMSEventStatus>
{
	public EbMSEventStatusType(int type)
	{
		super(type);
	}

	@Override
	public Class<EbMSEventStatus> getReturnedClass()
	{
		return EbMSEventStatus.class;
	}

	@Override
	public EbMSEventStatus getValue(ResultSet rs, int startIndex) throws SQLException
	{
		return EbMSEventStatus.values()[rs.getInt(startIndex)];
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, EbMSEventStatus value) throws SQLException
	{
		st.setInt(startIndex,value.getId());
	}
}
