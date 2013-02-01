/*******************************************************************************
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
 ******************************************************************************/
package nl.clockwork.ebms.processor;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;

import nl.clockwork.ebms.common.util.DOMUtils;
import nl.clockwork.ebms.model.EbMSAcknowledgment;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageError;
import nl.clockwork.ebms.model.EbMSStatusRequest;
import nl.clockwork.ebms.model.RawEbMSMessage;
import nl.clockwork.ebms.model.Signature;
import nl.clockwork.ebms.signing.EbMSSignatureValidator;
import nl.clockwork.ebms.util.EbMSMessageUtils;
import nl.clockwork.ebms.validation.ValidatorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class EbMSHttpHandlerImpl implements EbMSHttpHandler
{
  protected transient Log logger = LogFactory.getLog(getClass());
  private EbMSSignatureValidator signatureValidator;
	private EbMSMessageProcessor messageProcessor;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response) throws EbMSProcessorException
	{
	  try
		{
	  	EbMSMessage in = parseRequest(request);
			EbMSBaseMessage out = messageProcessor.process(in);
			if (out == null)
				response.setStatus(204);
			else
				createResponse(out,response);
		}
		catch (Exception e)
		{
			throw new EbMSProcessorException(e);
		}
	  
	}
	
	private EbMSMessage parseRequest(HttpServletRequest request) throws EbMSProcessorException, IOException, ValidatorException
	{
  	if (!"\"ebXML\"".equals(request.getHeader("SOAPAction")))
  		throw new EbMSProcessorException("Service not found");
  	EbMSMessageReader messageReader = new Mime4jEbMSMessageReader();
		RawEbMSMessage raw = messageReader.read(request.getContentType(),request.getInputStream());
		Signature signature = signatureValidator.validate(raw);
		EbMSMessage result = messageReader.read(raw);
		result.setSignature(signature);
		return result;
	}

	private void createResponse(EbMSBaseMessage ebMSMessage, HttpServletResponse response) throws IOException, SOAPException, JAXBException, ParserConfigurationException, SAXException, TransformerException
	{
		response.setStatus(200);
		response.setHeader("Content-Type","text/xml");
		response.setHeader("SOAPAction","\"ebXML\"");

		if (ebMSMessage instanceof EbMSMessageError)
			ebMSMessage = new EbMSMessage(ebMSMessage.getMessageHeader(),((EbMSMessageError)ebMSMessage).getErrorList());
		else if (ebMSMessage instanceof EbMSAcknowledgment)
			ebMSMessage = new EbMSMessage(ebMSMessage.getMessageHeader(),((EbMSAcknowledgment)ebMSMessage).getAcknowledgment());
		else if (ebMSMessage instanceof EbMSStatusRequest)
			ebMSMessage = new EbMSMessage(ebMSMessage.getMessageHeader(),null,((EbMSStatusRequest)ebMSMessage).getStatusRequest());
		else if (ebMSMessage instanceof EbMSMessageError)
			ebMSMessage = new EbMSMessage(ebMSMessage.getMessageHeader(),((EbMSMessageError)ebMSMessage).getErrorList());

		Document message = EbMSMessageUtils.createSOAPMessage((EbMSMessage)ebMSMessage);
		DOMUtils.write(message,response.getOutputStream());
	}

	public void setSignatureValidator(EbMSSignatureValidator signatureValidator)
	{
		this.signatureValidator = signatureValidator;
	}

	public void setMessageProcessor(EbMSMessageProcessor messageProcessor)
	{
		this.messageProcessor = messageProcessor;
	}

}
