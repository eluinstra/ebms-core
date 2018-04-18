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
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XSDValidator
{
	protected Schema schema;

	public XSDValidator(String xsdFile)
	{
		try
		{
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      String systemId = this.getClass().getResource(xsdFile).toString();
			schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream(xsdFile),systemId));
		}
		catch (SAXException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public void validate(String xml) throws ValidatorException, ValidationException
	{
		try
		{
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(new StringReader(xml)));
		}
		catch (SAXException e)
		{
			throw new ValidationException(e);
		}
		catch (IOException e)
		{
			throw new ValidatorException(e);
		}
	}

	public void validate(Node node) throws ValidatorException, ValidationException
	{
		try
		{
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(node));
		}
		catch (SAXException e)
		{
			throw new ValidationException(e);
		}
		catch (IOException e)
		{
			throw new ValidatorException(e);
		}
	}

}
