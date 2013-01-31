/*******************************************************************************
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
 ******************************************************************************/
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