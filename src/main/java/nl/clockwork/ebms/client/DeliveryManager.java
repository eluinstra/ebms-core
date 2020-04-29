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
import java.security.cert.CertificateException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.common.MessageQueue;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

public class DeliveryManager implements InitializingBean //DeliveryService
{
	protected transient Log logger = LogFactory.getLog(getClass());
	protected ExecutorService executorService;
	private Integer maxThreads;
	private Integer processorsScaleFactor;
	private Integer queueScaleFactor;
	private MessageQueue<EbMSMessage> messageQueue;
	private CPAManager cpaManager;
	private EbMSHttpClientFactory ebMSClientFactory;

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
		executorService = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
	}

	public Optional<EbMSMessage> sendMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = message.getMessageHeader();
			final String uri = getUri(messageHeader);
			if (message.getSyncReply() == null)
			{
				try
				{
					messageQueue.register(messageHeader.getMessageData().getMessageId());
					logger.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
					EbMSDocument document = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
					if (document == null)
						return messageQueue.get(messageHeader.getMessageData().getMessageId());
					else
					{
						messageQueue.remove(messageHeader.getMessageData().getMessageId());
						return Optional.of(EbMSMessageUtils.getEbMSMessage(document));
					}
				}
				catch (Exception e)
				{
					messageQueue.remove(messageHeader.getMessageData().getMessageId());
					throw e;
				}
			}
			else
			{
				logger.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				EbMSDocument response = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (response != null)
					return Optional.of(EbMSMessageUtils.getEbMSMessage(response));
			}
			return Optional.empty();
		}
		catch (SOAPException | JAXBException | SAXException | IOException | TransformerException | CertificateException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException | TransformerFactoryConfigurationError | XPathExpressionException e)
		{
			throw new EbMSProcessorException(e);
		}
	}

	protected String getUri(MessageHeader messageHeader)
	{
		return cpaManager.getUri(
				messageHeader.getCPAId(),
				new CacheablePartyId(messageHeader.getTo().getPartyId()),
				messageHeader.getTo().getRole(),
				CPAUtils.toString(messageHeader.getService()),
				messageHeader.getAction());
	}

	public void handleResponseMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		messageQueue.put(message.getMessageHeader().getMessageData().getRefToMessageId(),message);
	}
	
	public void sendResponseMessage(final String uri, final EbMSMessage response) throws EbMSProcessorException
	{
		Runnable command = () ->
		{
			try
			{
				MessageHeader messageHeader = response.getMessageHeader();
				logger.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				createClient(messageHeader).sendMessage(uri,response);
			}
			catch (Exception e)
			{
				logger.error("",e);
			}
		};
		executorService.execute(command);
	}

	protected EbMSClient createClient(MessageHeader messageHeader) throws CertificateException
	{
		DeliveryChannel sendDeliveryChannel = 
				cpaManager.getSendDeliveryChannel(
						messageHeader.getCPAId(),
						new CacheablePartyId(messageHeader.getFrom().getPartyId()),
						messageHeader.getFrom().getRole(),
						CPAUtils.toString(messageHeader.getService()),
						messageHeader.getAction())
				.orElse(null);
		return ebMSClientFactory.getEbMSClient(sendDeliveryChannel);
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

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setEbMSClientFactory(EbMSHttpClientFactory ebMSClientFactory)
	{
		this.ebMSClientFactory = ebMSClientFactory;
	}
}
