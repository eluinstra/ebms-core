package nl.clockwork.ebms.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import com.querydsl.sql.types.AbstractType;

public class InstantType extends AbstractType<Instant>
{
	public InstantType(int type)
	{
		super(type);
	}

	@Override
	public Class<Instant> getReturnedClass()
	{
		return Instant.class;
	}

	@Override
	public Instant getValue(ResultSet rs, int startIndex) throws SQLException
	{
		return toInstant(rs.getTimestamp(startIndex));
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, Instant value) throws SQLException
	{
		st.setTimestamp(startIndex,toTimestamp(value));
	}

	public static Instant toInstant(Timestamp timestamp)
	{
		return timestamp != null ? timestamp.toInstant() : null;
	}

	public static Timestamp toTimestamp(Instant instant)
	{
		return instant != null ? Timestamp.from(instant) : null;
	}
}
