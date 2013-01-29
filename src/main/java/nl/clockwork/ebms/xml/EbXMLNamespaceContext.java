package nl.clockwork.ebms.xml;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public class EbXMLNamespaceContext implements NamespaceContext
{

	public String getNamespaceURI(String prefix)
	{
		if (prefix == null)
			throw new NullPointerException("prefix is null");
		else if ("soap".equals(prefix))
			return "http://schemas.xmlsoap.org/soap/envelope/";
		else if ("ebxml".equals(prefix))
			return "http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd";
		else if ("ds".equals(prefix))
			return "http://www.w3.org/2000/09/xmldsig#";
		return XMLConstants.NULL_NS_URI;
	}

	public String getPrefix(String uri)
	{
		throw new UnsupportedOperationException();
	}

	public Iterator<Object> getPrefixes(String uri)
	{
		throw new UnsupportedOperationException();
	}

}