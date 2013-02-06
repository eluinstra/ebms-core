package nl.clockwork.mule.ebms.stub.ebf.processor;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nl.clockwork.ebms.iface.CPAService;
import nl.clockwork.ebms.iface.CPAServiceException;
import nl.clockwork.mule.common.component.Callable;

import org.mule.api.MuleMessage;
import org.xml.sax.SAXException;

public class CPAInserter extends Callable
{
	private Schema schema;
	private CPAService cpaService;
	private Object validatorMonitor = new Object();

	public CPAInserter() throws SAXException
	{
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		//schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream(xsdFile)));
    String systemId = this.getClass().getResource("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd").toString();
		schema = factory.newSchema(new StreamSource(this.getClass().getResourceAsStream("/nl/clockwork/ebms/xsd/cpp-cpa-2_0.xsd"),systemId));
	}
	
	@Override
	public Object onCall(MuleMessage message) throws Exception
	{
		try
		{
			String cpa = (String)message.getPayload();
			//quick fix for synchronization problem with validate() method
			synchronized (validatorMonitor)
			{
				Validator validator = schema.newValidator();
				try
				{
					validator.validate(new StreamSource(new StringReader(cpa)));
					cpaService.insertCPA(cpa,true);
					message.setProperty("EBMS.REPORT",message.getProperty("originalFilename") + " inserted successfully.");
				}
				catch (SAXException e)
				{
					message.setProperty("EBMS.REPORT",message.getProperty("originalFilename") + " contains not a valid CPA.");
				}
				return message;
			}
		}
		catch (CPAServiceException e)
		{
			Writer result = new StringWriter();
			if (message.getExceptionPayload() != null)
			{
				PrintWriter pw = new PrintWriter(result);
				e.printStackTrace(pw);
			}
			message.setProperty("EBMS.REPORT","Update " + message.getProperty("originalFilename") + " failed.\n\n" + result.toString());
			throw e;
		}
	}

	public void setCpaService(CPAService cpaService)
	{
		this.cpaService = cpaService;
	}
}
