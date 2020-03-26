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
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;

import nl.clockwork.ebms.dao.DAOException;
import nl.clockwork.ebms.model.EbMSDataSource;
import nl.clockwork.ebms.model.EbMSDataSourceMTOM;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.model.EbMSMessageContent;
import nl.clockwork.ebms.model.EbMSMessageContentMTOM;
import nl.clockwork.ebms.model.EbMSMessageContext;
import nl.clockwork.ebms.processor.EbMSProcessingException;
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
			EbMSMessageContent mc = toEbMSMessageContent(messageContent);
			final EbMSMessage message = ebMSMessageFactory.createEbMSMessage(mc.getContext().getCpaId(),mc);
			signatureGenerator.generate(message);
			storeMessage(message);
			return message.getMessageHeader().getMessageData().getMessageId();
		}
		catch (ValidatorException | DAOException | TransformerFactoryConfigurationError | EbMSProcessorException e)
		{
			throw new EbMSMessageServiceException(e);
		}
	}

	private EbMSMessageContent toEbMSMessageContent(EbMSMessageContentMTOM messageContent) throws EbMSProcessingException
	{
		EbMSMessageContext context = messageContent.getContext();
		List<EbMSDataSource> dataSources = messageContent.getDataSources().stream().map(ds -> toEbMSDataSource(ds)).collect(Collectors.toList());
		return new EbMSMessageContent(context,dataSources);
	}

	private EbMSDataSource toEbMSDataSource(EbMSDataSourceMTOM ds) throws EbMSProcessingException
	{
		try
		{
			try
			{
				ContentType contentType = new ContentType(ds.getAttachment().getContentType());
				return new EbMSDataSource(contentType.getParameter("name"),ds.getContentId(),contentType.getBaseType(),IOUtils.toByteArray(ds.getAttachment().getInputStream()));
			}
			catch (ParseException e)
			{
				return new EbMSDataSource(null,ds.getContentId(),ds.getAttachment().getContentType(),IOUtils.toByteArray(ds.getAttachment().getInputStream()));
			}
		}
		catch (IOException e)
		{
			throw new EbMSProcessingException();
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
