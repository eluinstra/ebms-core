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
package nl.clockwork.ebms.service;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import lombok.NonNull;
import lombok.val;
import nl.clockwork.ebms.EbMSMessageFactory;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.client.DeliveryManager;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.event.processor.EventManager;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.signing.EbMSSignatureGenerator;
import nl.clockwork.ebms.validation.EbMSMessageContextValidator;
import nl.clockwork.ebms.validation.ValidatorException;

public class EbMSMessageServiceMTOMImpl extends EbMSMessageServiceImpl implements EbMSMessageServiceMTOM
{
	public EbMSMessageServiceMTOMImpl(
			@NonNull DeliveryManager deliveryManager,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSMessageFactory ebMSMessageFactory,
			@NonNull EventManager eventManager,
			@NonNull EbMSMessageContextValidator ebMSMessageContextValidator,
			@NonNull EbMSSignatureGenerator signatureGenerator,
			boolean deleteEbMSAttachmentsOnMessageProcessed)
	{
		super(deliveryManager,ebMSDAO,cpaManager,ebMSMessageFactory,eventManager,ebMSMessageContextValidator,signatureGenerator,deleteEbMSAttachmentsOnMessageProcessed);
	}

	@Override
	public String sendMessageMTOM(EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(messageContent.getContext());
			val message = ebMSMessageFactory.createEbMSMessageMTOM(messageContent);
			val document = EbMSMessageUtils.getEbMSDocument(message);
			signatureGenerator.generate(document,message);
			storeMessage(document.getMessage(),message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	@Override
	public EbMSMessageContentMTOM getMessageMTOM(String messageId, Boolean process) throws EbMSMessageServiceException
	{
		try
		{
			if (process != null && process)
				processMessage(messageId);
			return ebMSDAO.getMessageContentMTOM(messageId).orElse(null);
		}
		catch (DAOException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}
}
