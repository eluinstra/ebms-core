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

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.scheduling.annotation.Async;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSRequestMessage;
import nl.clockwork.ebms.model.EbMSResponseMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryManager
{
	@NonNull
	MessageQueue<EbMSResponseMessage> messageQueue;
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;

	@Builder
	public DeliveryManager(
			@NonNull MessageQueue<EbMSResponseMessage> messageQueue,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory)
	{
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
				messageHeader.getTo().getPartyId(),
				messageHeader.getTo().getRole(),
				CPAUtils.toString(messageHeader.getService()),
				messageHeader.getAction());
	}

	public void handleResponseMessage(final EbMSResponseMessage message) throws EbMSProcessorException
	{
		messageQueue.put(message.getMessageHeader().getMessageData().getRefToMessageId(),message);
	}

	@Async("deliveryManagerTaskExecutor")
	public void sendResponseMessage(final String uri, final EbMSBaseMessage response) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = response.getMessageHeader();
			log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
			createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
		}
		catch (EbMSProcessorException e)
		{
			throw e;
		}
		catch (CertificateException | SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
	}

	protected EbMSClient createClient(MessageHeader messageHeader) throws CertificateException
	{
		String cpaId = messageHeader.getCPAId();
		val sendDeliveryChannel = 
				cpaManager.getSendDeliveryChannel(
						cpaId,
						messageHeader.getFrom().getPartyId(),
						messageHeader.getFrom().getRole(),
						CPAUtils.toString(messageHeader.getService()),
						messageHeader.getAction())
				.orElse(null);
		return ebMSClientFactory.getEbMSClient(cpaId,sendDeliveryChannel);
	}
}
