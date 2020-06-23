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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.querydsl.sql.types.AbstractType;

import nl.clockwork.ebms.util.DOMUtils;

public class DocumentType extends AbstractType<Document>
{
	public DocumentType(int type)
	{
		super(type);
	}

	@Override
	public Class<Document> getReturnedClass()
	{
		return Document.class;
	}

	@Override
	public Document getValue(ResultSet rs, int startIndex) throws SQLException
	{
		try
		{
			return DOMUtils.read(rs.getString(startIndex));
		}
		catch (ParserConfigurationException | SAXException | IOException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public void setValue(PreparedStatement st, int startIndex, Document value) throws SQLException
	{
		try
		{
			st.setString(startIndex,DOMUtils.toString(value));
		}
		catch (TransformerException e)
		{
			throw new SQLException(e);
		}
	}
}
