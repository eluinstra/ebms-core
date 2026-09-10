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
package nl.clockwork.ebms.common.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XSDValidator
{
	// Matches schemaLocation="..." on the xs:import / xs:include elements that the bundled XSDs use to
	// reference sibling schemas (e.g. xlink.xsd, xmldsig-core-schema.xsd, xml.xsd).
	private static final Pattern SCHEMA_LOCATION = Pattern.compile("schemaLocation\\s*=\\s*[\"']([^\"']+)[\"']");

	Schema schema;

	@Builder()
	public XSDValidator(String xsdFile)
	{
		try
		{
			val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			// F6: block external schema access (file:/http:) so an attacker-supplied document cannot
			// import a local file or a remote schema (local-file-read / SSRF-adjacent). The bundled XSDs
			// reference sibling schemas by relative schemaLocation (which resolve to jar: URLs, also
			// blocked by the empty setting), so those are pre-loaded from the classpath below and the
			// parser never needs to fetch them.
			factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			val base = this.getClass().getResource(xsdFile);
			val sources = new ArrayList<StreamSource>();
			// Pre-load every relative sibling import/include from the classpath so they are resolved
			// internally instead of being treated as (and rejected as) external resources.
			sources.addAll(preloadClasspathSchemas(base));
			sources.add(new StreamSource(base.openStream(), base.toString()));
			schema = factory.newSchema(sources.toArray(new StreamSource[0]));
		}
		catch (SAXException | IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static List<StreamSource> preloadClasspathSchemas(URL base)
	{
		val sources = new ArrayList<StreamSource>();
		try
		{
			val text = IOUtils.toString(base.openStream(), java.nio.charset.StandardCharsets.UTF_8);
			val matcher = SCHEMA_LOCATION.matcher(text);
			while (matcher.find())
			{
				val location = matcher.group(1);
				// Only relative (non-absolute) references are resolved; absolute http(s)/file locations
				// are deliberately left to the accessExternalSchema restriction, which keeps them blocked.
				if (location.startsWith("http") || location.startsWith("file:") || location.startsWith("jar:"))
					continue;
				URL url;
				try
				{
					url = new URL(base, location);
				}
				catch (java.net.MalformedURLException e)
				{
					continue;
				}
				try
				{
					IOUtils.closeQuietly(url.openStream());
					sources.add(new StreamSource(url.openStream(), url.toString()));
				}
				catch (IOException e)
				{
					// A missing sibling is not fatal here; the parser will simply report it when it
					// actually needs the imported namespace.
				}
			}
		}
		catch (IOException e)
		{
			// If the base schema cannot be read as text, fall back to loading it as a single source only.
		}
		return sources;
	}

	public void validate(String xml) throws SAXException, IOException
	{
		val validator = schema.newValidator();
		validator.validate(new StreamSource(new StringReader(xml)));
	}

	public void validate(Node node) throws SAXException, IOException
	{
		val validator = schema.newValidator();
		validator.validate(new DOMSource(node));
	}
}
