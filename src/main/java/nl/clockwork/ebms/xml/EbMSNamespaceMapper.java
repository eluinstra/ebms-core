package nl.clockwork.ebms.xml;

public class EbMSNamespaceMapper extends com.sun.xml.bind.marshaller.NamespacePrefixMapper
{

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix)
	{
		if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceUri))
			return "soap";
		else if ("http://www.w3.org/1999/xlink".equals(namespaceUri))
			return "xlink";
		else if ("http://www.w3.org/2000/09/xmldsig#".equals(namespaceUri))
			return "ds";
		else if ("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd".equals(namespaceUri))
			return "eb";
		return suggestion;
	}
	
	@Override
	public String[] getPreDeclaredNamespaceUris()
	{
		return new String[]{"http://schemas.xmlsoap.org/soap/envelope/","http://www.w3.org/1999/xlink","http://www.w3.org/2000/09/xmldsig#","http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"};
	}

}
