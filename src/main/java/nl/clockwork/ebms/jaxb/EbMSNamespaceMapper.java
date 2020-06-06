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
package nl.clockwork.ebms.jaxb;

import static io.vavr.API.*;

public class EbMSNamespaceMapper extends com.sun.xml.bind.marshaller.NamespacePrefixMapper
{
	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix)
	{
		return Match(namespaceUri).of(
				Case($("http://schemas.xmlsoap.org/soap/envelope/"),"soap"),
				Case($("http://www.w3.org/1999/xlink"),"xlink"),
				Case($("http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"),"eb"),
				Case($(),suggestion));
	}
	
	@Override
	public String[] getPreDeclaredNamespaceUris()
	{
		return new String[]{"http://schemas.xmlsoap.org/soap/envelope/","http://www.w3.org/1999/xlink","http://www.oasis-open.org/committees/ebxml-msg/schema/msg-header-2_0.xsd"};
	}
}
