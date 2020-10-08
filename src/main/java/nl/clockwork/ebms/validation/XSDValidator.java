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
package nl.clockwork.ebms.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.val;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XSDValidator
{
	Schema schema;

	@Builder()
	public XSDValidator(String xsdFile)
	{
		try
		{
			val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      val systemId = this.getClass().getResource(xsdFile).toString();
			schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream(xsdFile),systemId));
		}
		catch (SAXException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void validate(String xml) throws SAXException, IOException
	{
		val validator = getValidator();
		validator.validate(new StreamSource(new StringReader(xml)));
	}

	public void validate(InputStream is) throws SAXException, IOException
	{
		val validator = getValidator();
		validator.validate(new StreamSource(is));
	}

	private javax.xml.validation.Validator getValidator() throws SAXNotRecognizedException, SAXNotSupportedException
	{
		val validator = schema.newValidator();
		validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD,"");
		validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
		return validator;
	}
}
