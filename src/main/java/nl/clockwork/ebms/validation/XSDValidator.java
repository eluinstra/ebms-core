package nl.clockwork.ebms.validation;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XSDValidator
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Schema schema;

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
			//validator.validate(new SAXSource(new InputSource(new StringReader(xml))));
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

	public void validate(Document document) throws ValidatorException, ValidationException
	{
		try
		{
			Validator validator = schema.newValidator();
			validator.validate(new DOMSource(document));
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
