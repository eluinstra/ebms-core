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
package nl.clockwork.ebms.client;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.common.MessageQueue;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

public class DeliveryManager implements InitializingBean //DeliveryService
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected ExecutorService executorService;
	private Integer maxThreads;
	private Integer processorsScaleFactor;
	private Integer queueScaleFactor;
	private MessageQueue<EbMSMessage> messageQueue;
	protected EbMSClient ebMSClient;

	@Override
	public void afterPropertiesSet() throws Exception
	{
		//executorService = Executors.newFixedThreadPool(maxThreads);
		if (processorsScaleFactor == null || processorsScaleFactor <= 0)
		{
			processorsScaleFactor = 1;
			logger.info(this.getClass().getName() + " using processors scale factor " + processorsScaleFactor);
		}
		if (maxThreads == null || maxThreads <= 0)
		{
			maxThreads = Runtime.getRuntime().availableProcessors() * processorsScaleFactor;
			logger.info(this.getClass().getName() + " using " + maxThreads + " threads");
		}
		if (queueScaleFactor == null || queueScaleFactor <= 0)
		{
			queueScaleFactor = 1;
			logger.info(this.getClass().getName() + " using queue scale factor " + queueScaleFactor);
		}
		executorService = new ThreadPoolExecutor(maxThreads,maxThreads,1,TimeUnit.MINUTES,new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public Optional<EbMSMessage> sendMessage(final String uri, final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			if (message.getSyncReply() == null)
			{
				try
				{
					messageQueue.register(message.getMessageHeader().getMessageData().getMessageId());
					logger.info("Sending message " + message.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
					EbMSDocument document = ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
					if (document == null)
						return messageQueue.get(message.getMessageHeader().getMessageData().getMessageId());
					else
					{
						messageQueue.remove(message.getMessageHeader().getMessageData().getMessageId());
						return Optional.of(EbMSMessageUtils.getEbMSMessage(document));
					}
				}
				catch (Exception e)
				{
					messageQueue.remove(message.getMessageHeader().getMessageData().getMessageId());
					throw e;
				}
			}
			else
			{
				logger.info("Sending message " + message.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
				EbMSDocument response = ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (response != null)
					return Optional.of(EbMSMessageUtils.getEbMSMessage(response));
			}
			return Optional.empty();
		}
		catch (SOAPException | JAXBException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException | TransformerFactoryConfigurationError | XPathExpressionException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	public void handleResponseMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		messageQueue.put(message.getMessageHeader().getMessageData().getRefToMessageId(),message);
	}
	
	public void sendResponseMessage(final String uri, final EbMSMessage response) throws EbMSProcessorException
	{
		Runnable command = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					logger.info("Sending message " + response.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
					ebMSClient.sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
				}
				catch (Exception e)
				{
					logger.error("",e);
				}
			}
		};
		executorService.execute(command);
	}

	public void setMaxThreads(Integer maxThreads)
	{
		this.maxThreads = maxThreads;
	}

	public void setProcessorsScaleFactor(Integer processorsScaleFactor)
	{
		this.processorsScaleFactor = processorsScaleFactor;
	}

	public void setQueueScaleFactor(Integer queueScaleFactor)
	{
		this.queueScaleFactor = queueScaleFactor;
	}

	public void setMessageQueue(MessageQueue<EbMSMessage> messageQueue)
	{
		this.messageQueue = messageQueue;
	}

	public void setEbMSClient(EbMSClient ebMSClient)
	{
		this.ebMSClient = ebMSClient;
	}

}
