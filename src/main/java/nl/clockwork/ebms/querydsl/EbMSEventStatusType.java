package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.querydsl.sql.types.AbstractType;

import lombok.val;
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
		val id = rs.getObject(startIndex,Integer.class);
		return id != null ? EbMSEventStatus.get(id).orElseThrow(() -> new IllegalArgumentException("EbMSEventStatus " + id + " is not valid!")) : null;
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, EbMSEventStatus value) throws SQLException
	{
		st.setInt(startIndex,value != null ? value.getId() : null);
	}
}
