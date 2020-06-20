package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.querydsl.sql.types.AbstractType;

import lombok.val;
import nl.clockwork.ebms.EbMSMessageStatus;

public class EbMSMessageStatusType extends AbstractType<EbMSMessageStatus>
{
	public EbMSMessageStatusType(int type)
	{
		super(type);
	}

	@Override
	public Class<EbMSMessageStatus> getReturnedClass()
	{
		return EbMSMessageStatus.class;
	}

	@Override
	public EbMSMessageStatus getValue(ResultSet rs, int startIndex) throws SQLException
	{
		val id = rs.getObject(startIndex,Integer.class);
		return id != null ? EbMSMessageStatus.get(id).orElseThrow(() -> new IllegalArgumentException("" + id + " is not a valid EbMSMessageStatus id!")) : null;
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, EbMSMessageStatus value) throws SQLException
	{
		st.setInt(startIndex,value != null ? value.getId() : null);
	}
}
