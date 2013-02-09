package nl.clockwork.mule.ebms.stub.ebf.processor;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import nl.clockwork.ebms.iface.CPAService;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transport.PropertyScope;
import org.xml.sax.SAXException;

public class CPAInserter implements Callable
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
	public Object onCall(MuleEventContext eventContext) throws Exception
	{
		MuleMessage message = eventContext.getMessage();
		String cpa = (String)message.getPayload();
		//quick fix for synchronization problem with validate() method
		synchronized (validatorMonitor)
		{
			Validator validator = schema.newValidator();
			try
			{
				validator.validate(new StreamSource(new StringReader(cpa)));
				cpaService.insertCPA(cpa,true);
				message.setProperty("EBMS.REPORT",message.getProperty("originalFilename",PropertyScope.OUTBOUND) + " inserted successfully.",PropertyScope.SESSION);
			}
			catch (SAXException e)
			{
				message.setProperty("EBMS.REPORT",message.getProperty("originalFilename",PropertyScope.OUTBOUND) + " contains not a valid CPA.",PropertyScope.SESSION);
			}
			return message;
		}
	}

	public void setCpaService(CPAService cpaService)
	{
		this.cpaService = cpaService;
	}
}
