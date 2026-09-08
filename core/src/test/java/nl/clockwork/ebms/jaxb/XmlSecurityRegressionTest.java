/*
 * Copyright 2011 - 2026 Clockwork
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import nl.clockwork.ebms.util.DOMUtils;
import nl.clockwork.ebms.validation.XSDValidator;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Regression tests guarding every untrusted-XML entry point against the
 * billion-laughs / XXE attack (exponential internal-entity expansion).
 *
 * <p>A "billion laughs" payload is a DOCTYPE with escalating internal entities;
 * it only needs to be rejected (by disallow-doctype-decl) and must never expand
 * to a huge in-memory value.
 */
class XmlSecurityRegressionTest
{
	@XmlRootElement(name = "test")
	public static class Payload
	{
		@XmlAttribute(name = "attr")
		public String attr;
	}

	/** Escalating internal-entity bomb: the final reference expands to 1,000,000 characters. */
	private static String bomb(String root)
	{
		return "<!DOCTYPE " + root + " [\n"
				+ " <!ENTITY a \"xxxxxxxxxx\">\n"
				+ " <!ENTITY b \"&a;&a;&a;&a;&a;&a;&a;&a;&a;&a;\">\n"
				+ " <!ENTITY c \"&b;&b;&b;&b;&b;&b;&b;&b;&b;&b;\">\n"
				+ " <!ENTITY d \"&c;&c;&c;&c;&c;&c;&c;&c;&c;&c;\">\n"
				+ " <!ENTITY e \"&d;&d;&d;&d;&d;&d;&d;&d;&d;&d;\">\n"
				+ " <!ENTITY f \"&e;&e;&e;&e;&e;&e;&e;&e;&e;&e;\">\n"
				+ "]\n"
				+ "<" + root + " attr=\"&f;\"></" + root + ">";
	}

	/** Minimal DOCTYPE (no large expansion) used to build a DOM that still carries a DOCTYPE. */
	private static String doctypeDoc(String root)
	{
		return "<!DOCTYPE " + root + " [<!ENTITY a \"x\">]>\n" + "<" + root + " attr=\"&a;\"></" + root + ">";
	}

	@Test
	void handleStringRejectsBillionLaughs()
	{
		int len;
		try
		{
			Payload t = JAXBParser.getInstance(Payload.class).handle(bomb("test"));
			len = (t == null || t.attr == null) ? 0 : t.attr.length();
		}
		catch (Exception e)
		{
			System.out.println("[handle(String)] bomb rejected with " + e.getClass().getSimpleName());
			return;
		}
		assertTrue(len < 100000, "billion-laughs bomb expanded to " + len + " chars via handle(String)");
	}

	@Test
	void handleStringStillParsesBenignXml() throws Exception
	{
		Payload t = JAXBParser.getInstance(Payload.class).handle("<test attr=\"hello\"></test>");
		assertNotNull(t);
		assertEquals("hello", t.attr);
	}

	@Test
	void handleNodeRejectsDoctypeDocument() throws Exception
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new InputSource(new StringReader(doctypeDoc("test"))));
		assertNotNull(doc.getDoctype(), "precondition: document carries a DOCTYPE");
		JAXBException e = assertThrows(JAXBException.class, () -> JAXBParser.getInstance(Payload.class).handle(doc));
		assertEquals("Unsafe XML parsing is not allowed", e.getMessage());
	}

	@Test
	void xsdValidatorStringRejectsBillionLaughs()
	{
		XSDValidator v = new XSDValidator("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd");
		assertThrows(SAXException.class, () -> v.validate(bomb("CollaborationProtocolAgreement")));
	}

	@Test
	void domUtilsReadRejectsBillionLaughs()
	{
		assertThrows(SAXException.class, () -> DOMUtils.read(new ByteArrayInputStream(bomb("test").getBytes())));
	}
}
