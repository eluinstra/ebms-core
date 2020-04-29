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

import javax.xml.transform.TransformerFactoryConfigurationError;

import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.validation.ValidatorException;

public class EbMSMessageServiceMTOMImpl extends EbMSMessageServiceImpl implements InitializingBean, EbMSMessageServiceMTOM
{
	@Override
	public String sendMessageMTOM(EbMSMessageContentMTOM messageContent) throws EbMSMessageServiceException
	{
		try
		{
			ebMSMessageContextValidator.validate(messageContent.getContext());
			final EbMSMessage message = ebMSMessageFactory.createEbMSMessageMTOM(messageContent);
			signatureGenerator.generate(message);
			storeMessage(message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException e)
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
