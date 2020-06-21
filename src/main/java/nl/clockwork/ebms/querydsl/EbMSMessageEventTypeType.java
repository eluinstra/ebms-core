package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.querydsl.sql.types.AbstractType;

import lombok.val;
import nl.clockwork.ebms.event.listener.EbMSMessageEventType;

public class EbMSMessageEventTypeType extends AbstractType<EbMSMessageEventType>
{
	public EbMSMessageEventTypeType(int type)
	{
		super(type);
	}

	@Override
	public Class<EbMSMessageEventType> getReturnedClass()
	{
		return EbMSMessageEventType.class;
	}

	@Override
	public EbMSMessageEventType getValue(ResultSet rs, int startIndex) throws SQLException
	{
		val id = rs.getObject(startIndex,Integer.class);
		return id != null ? EbMSMessageEventType.get(id).orElseThrow(() -> new IllegalArgumentException("EbMSMessageEventType " + id + " is not valid!")) : null;
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, EbMSMessageEventType value) throws SQLException
	{
		st.setInt(startIndex,value != null ? value.getId() : null);
	}
}
