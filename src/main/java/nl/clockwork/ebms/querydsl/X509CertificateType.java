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

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.querydsl.sql.types.AbstractType;

public class X509CertificateType extends AbstractType<X509Certificate>
{
	public X509CertificateType()
	{
		this(Types.BLOB);
	}
	public X509CertificateType(int type)
	{
		super(type);
	}

	@Override
	public Class<X509Certificate> getReturnedClass()
	{
		return X509Certificate.class;
	}

	@Override
	public X509Certificate getValue(ResultSet rs, int startIndex) throws SQLException
	{
		try
		{
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
			return (X509Certificate)certificateFactory.generateCertificate(rs.getBinaryStream(startIndex));
		}
		catch (CertificateException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, X509Certificate value) throws SQLException
	{
		try
		{
			st.setBytes(startIndex,value.getEncoded());
		}
		catch (CertificateEncodingException e)
		{
			throw new SQLException(e);
		}
	}
}
