package nl.clockwork.ebms.querydsl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.cxf.io.CachedOutputStream;

import com.querydsl.sql.types.AbstractType;

import lombok.val;

public class CachedOutputStreamType extends AbstractType<CachedOutputStream>
{
	public CachedOutputStreamType(int type)
	{
		super(type);
	}

	@Override
	public Class<CachedOutputStream> getReturnedClass()
	{
		return CachedOutputStream.class;
	}

	@Override
	public CachedOutputStream getValue(ResultSet rs, int startIndex) throws SQLException
	{
		try
		{
			return createCachedOutputStream(rs.getBinaryStream("content"));
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, CachedOutputStream value) throws SQLException
	{
		try (val a = value)
		{
			st.setBinaryStream(startIndex,a.getInputStream());
		}
		catch (IOException e)
		{
			throw new SQLException(e);
		}
	}

	public static CachedOutputStream createCachedOutputStream(InputStream in) throws IOException
	{
		val result = new CachedOutputStream();
		CachedOutputStream.copyStream(in,result,4096);
		result.lockOutputStream();
		return result;
	}
}
