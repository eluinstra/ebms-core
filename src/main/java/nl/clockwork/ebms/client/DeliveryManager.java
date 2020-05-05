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

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.common.MessageQueue;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSRequestMessage;
import nl.clockwork.ebms.model.EbMSResponseMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryManager //DeliveryService
{
	@NonNull
	MessageQueue<EbMSResponseMessage> messageQueue;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	protected ExecutorService executorService;

	public DeliveryManager(MessageQueue<EbMSResponseMessage> messageQueue, CPAManager cpaManager, EbMSHttpClientFactory ebMSClientFactory)
	{
		this(null,null,null,messageQueue,cpaManager,ebMSClientFactory);
	}

	@Builder(setterPrefix = "set")
	public DeliveryManager(
			Integer maxThreads,
			Integer processorsScaleFactor,
			Integer queueScaleFactor,
			@NonNull MessageQueue<EbMSResponseMessage> messageQueue,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory)
	{
		//executorService = Executors.newFixedThreadPool(maxThreads);
		if (processorsScaleFactor == null || processorsScaleFactor <= 0)
		{
			processorsScaleFactor = 1;
			log.info(this.getClass().getName() + " using processors scale factor " + processorsScaleFactor);
		}
		if (maxThreads == null || maxThreads <= 0)
		{
			maxThreads = Runtime.getRuntime().availableProcessors() * processorsScaleFactor;
			log.info(this.getClass().getName() + " using " + maxThreads + " threads");
		}
		if (queueScaleFactor == null || queueScaleFactor <= 0)
		{
			queueScaleFactor = 1;
			log.info(this.getClass().getName() + " using queue scale factor " + queueScaleFactor);
		}
		executorService = new ThreadPoolExecutor(
				maxThreads,
				maxThreads,
				1,
				TimeUnit.MINUTES,
				new ArrayBlockingQueue<>(maxThreads * queueScaleFactor,true),
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.messageQueue = messageQueue;
		this.cpaManager = cpaManager;
		this.ebMSClientFactory = ebMSClientFactory;
	}

	public Optional<EbMSResponseMessage> sendMessage(final EbMSRequestMessage message) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			val uri = getUri(messageHeader);
			if (message.getSyncReply() == null)
			{
				try
				{
					messageQueue.register(messageHeader.getMessageData().getMessageId());
					log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
					val response = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
					if (response == null)
						return messageQueue.get(messageHeader.getMessageData().getMessageId());
					else
					{
						messageQueue.remove(messageHeader.getMessageData().getMessageId());
						return Optional.of((EbMSResponseMessage)EbMSMessageUtils.getEbMSMessage(response));
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
				log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				val response = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (response != null)
					return Optional.of((EbMSResponseMessage)EbMSMessageUtils.getEbMSMessage(response));
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

	public void handleResponseMessage(final EbMSResponseMessage message) throws EbMSProcessorException
	{
		messageQueue.put(message.getMessageHeader().getMessageData().getRefToMessageId(),message);
	}
	
	public void sendResponseMessage(final String uri, final EbMSBaseMessage response) throws EbMSProcessorException
	{
		Runnable command = () ->
		{
			try
			{
				val messageHeader = response.getMessageHeader();
				log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
			}
			catch (Exception e)
			{
				log.error("",e);
			}
		};
		executorService.execute(command);
	}

	protected EbMSClient createClient(MessageHeader messageHeader) throws CertificateException
	{
		val sendDeliveryChannel = 
				cpaManager.getSendDeliveryChannel(
						messageHeader.getCPAId(),
						new CacheablePartyId(messageHeader.getFrom().getPartyId()),
						messageHeader.getFrom().getRole(),
						CPAUtils.toString(messageHeader.getService()),
						messageHeader.getAction())
				.orElse(null);
		return ebMSClientFactory.getEbMSClient(sendDeliveryChannel);
	}
}
