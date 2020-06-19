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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import javax.xml.bind.JAXBException;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.CollaborationProtocolAgreement;

import com.querydsl.sql.types.AbstractType;

import nl.clockwork.ebms.jaxb.JAXBParser;

public class CollaborationProtocolAgreementType extends AbstractType<CollaborationProtocolAgreement>
{
	public CollaborationProtocolAgreementType()
	{
		this(Types.BLOB);
	}
	public CollaborationProtocolAgreementType(int type)
	{
		super(type);
	}

	@Override
	public Class<CollaborationProtocolAgreement> getReturnedClass()
	{
		return CollaborationProtocolAgreement.class;
	}

	@Override
	public CollaborationProtocolAgreement getValue(ResultSet rs, int startIndex) throws SQLException
	{
		try
		{
			return JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(rs.getString(startIndex));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, CollaborationProtocolAgreement value) throws SQLException
	{
		try
		{
			st.setString(startIndex,JAXBParser.getInstance(CollaborationProtocolAgreement.class).handle(value));
		}
		catch (JAXBException e)
		{
			throw new SQLException(e);
		}
	}
}
