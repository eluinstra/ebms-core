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
package nl.clockwork.mule.common.transformer;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;

public abstract class AbstractXSLTransformer extends AbstractMessageTransformer
{
  protected transient Log logger = LogFactory.getLog(getClass());
	private Templates templates;
	private Map<String,String> parameters = new HashMap<String,String>();

	public AbstractXSLTransformer()
	{
	}
	
	@Override
	public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
	{
		try
		{
			Transformer transformer = templates.newTransformer();
			for (String name : parameters.keySet())
				transformer.setParameter(name,parameters.get(name));
			StreamSource xmlsource = new StreamSource(new StringReader(message.toString()));
			Writer writer = new StringWriter();
			StreamResult output = new StreamResult(writer);
			transformer.transform(xmlsource,output);
			writer.flush();
			message.setPayload(writer.toString());
			return message;
		}
		catch (Exception e)
		{
			throw new TransformerException(this,e);
		}
	}

	protected abstract String getContent(MuleMessage message) throws Exception;
	
	public void setXslFile(String xslFile)
	{
		try
		{
			TransformerFactory factory = TransformerFactory.newInstance();
			templates = factory.newTemplates(new StreamSource(this.getClass().getResourceAsStream(xslFile)));
		}
		catch (TransformerConfigurationException e)
		{
			logger.fatal("Error creating transformer using file " + xslFile,e);
			throw new RuntimeException(e);
		}
	}
	
	public void setParameters(Map<String,String> parameters)
	{
		this.parameters = parameters;
	}
}
