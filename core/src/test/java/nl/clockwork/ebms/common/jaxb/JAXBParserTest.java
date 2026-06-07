/*
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
package nl.clockwork.ebms.common.jaxb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;

class JAXBParserTest
{
	@Test
	void shouldHandleSimpleXmlThroughUnsafeStringApi() throws Exception
	{
		var parser = JAXBParser.getInstance(SampleMessage.class);
		var result = parser.handleUnsafe("<sampleMessage><value>ok</value></sampleMessage>");

		assertNotNull(result);
		assertEquals("ok", result.value);
	}

	@Test
	void shouldRejectDocTypeInUnsafeStringApi() throws Exception
	{
		var parser = JAXBParser.getInstance(SampleMessage.class);
		var xml = "<!DOCTYPE sampleMessage [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>" + "<sampleMessage><value>&xxe;</value></sampleMessage>";

		assertThrows(JAXBException.class, () -> parser.handleUnsafe(xml));
	}

	@XmlRootElement(name = "sampleMessage")
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class SampleMessage
	{
		@XmlElement
		String value;
	}
}
