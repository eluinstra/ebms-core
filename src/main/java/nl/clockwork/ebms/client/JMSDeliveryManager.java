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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.xml.sax.SAXException;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.apachecommons.CommonsLog;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.common.MessageQueue;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

@CommonsLog
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JMSDeliveryManager extends DeliveryManager
{
	private static final String MESSAGE = "MESSAGE";
	@NonNull
	JmsTemplate jmsTemplate;

	public JMSDeliveryManager(
			Integer maxThreads,
			Integer processorsScaleFactor,
			Integer queueScaleFactor,
			MessageQueue<EbMSMessage> messageQueue,
			CPAManager cpaManager,
			EbMSHttpClientFactory ebMSClientFactory,
			@NonNull JmsTemplate jmsTemplate)
	{
		super(maxThreads,processorsScaleFactor,queueScaleFactor,messageQueue,cpaManager,ebMSClientFactory);
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public Optional<EbMSMessage> sendMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			val uri = getUri(messageHeader);
			if (message.getSyncReply() == null)
			{
				log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				val document = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (document == null)
				{
					jmsTemplate.setReceiveTimeout(3 * Constants.MINUTE_IN_MILLIS);
					return Optional.ofNullable((EbMSMessage)jmsTemplate.receiveSelectedAndConvert(MESSAGE,"JMSCorrelationID='" + messageHeader.getMessageData().getMessageId() + "'"));
				}
				else
					return Optional.of(EbMSMessageUtils.getEbMSMessage(document));
			}
			else
			{
				log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				val response = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
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

	@Override
	public void handleResponseMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		jmsTemplate.setExplicitQosEnabled(true);
		jmsTemplate.setTimeToLive(Constants.MINUTE_IN_MILLIS);
		jmsTemplate.convertAndSend(MESSAGE,message,new MessagePostProcessor()
		{
			@Override
			public Message postProcessMessage(Message m) throws JMSException
			{
				m.setJMSCorrelationID(message.getMessageHeader().getMessageData().getRefToMessageId());
				//m.setJMSExpiration(Constants.MINUTE_IN_MILLIS);
				return m;
			}
		});
	}

	@Override
	public void sendResponseMessage(final String uri, final EbMSMessage response) throws EbMSProcessorException
	{
		Runnable command = () ->
		{
			try
			{
				log.info("Sending message " + response.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
				createClient(response.getMessageHeader()).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
			}
			catch (Exception e)
			{
				log.error("",e);
			}
		};
		executorService.execute(command);
	}
}
