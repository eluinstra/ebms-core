/**
 * Copyright 2011 Clockwork
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
