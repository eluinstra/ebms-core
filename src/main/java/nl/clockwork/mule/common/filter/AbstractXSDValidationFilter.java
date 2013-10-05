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
package nl.clockwork.mule.common.filter;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.xml.sax.SAXException;

public abstract class AbstractXSDValidationFilter implements Filter
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Schema schema;

	@Override
	public boolean accept(MuleMessage message)
	{
		try
		{
			String content = getContent(message);
			Validator validator = schema.newValidator();
			//validator.validate(new SAXSource(new InputSource(new StringReader(content))));
			validator.validate(new StreamSource(new StringReader(content)));
			return true;
		}
		catch (SAXException e)
		{
			logger.info("",e);
			return false;
		}
		catch (IOException e)
		{
			logger.error("",e);
			return false;
		}
		catch (Exception e)
		{
			logger.warn("",e);
			return false;
		}
	}

	protected abstract String getContent(MuleMessage message) throws Exception;

	public void setXsdFile(String xsdFile)
	{
		try
		{
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			//schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream(xsdFile)));
      String systemId = this.getClass().getResource(xsdFile).toString();
			schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream(xsdFile),systemId));
		}
		catch (SAXException e)
		{
			logger.fatal("",e);
			throw new RuntimeException(e);
		}
	}
}
