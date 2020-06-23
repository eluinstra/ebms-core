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
