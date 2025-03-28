/*
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
package nl.clockwork.ebms.delivery;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.delivery.client.EbMSHttpClientFactory;
import nl.clockwork.ebms.model.EbMSBaseMessage;
import nl.clockwork.ebms.model.EbMSRequestMessage;
import nl.clockwork.ebms.model.EbMSResponseMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.PlatformTransactionManager;
import org.xml.sax.SAXException;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JMSDeliveryManager extends DeliveryManager
{
	private static final String JMS_DESTINATION_NAME = "MESSAGE";
	@NonNull
	PlatformTransactionManager transactionManager;
	@NonNull
	JmsTemplate jmsTemplate;

	@Builder(builderMethodName = "jmsDeliveryManagerBuilder")
	public JMSDeliveryManager(
			@NonNull MessageQueue<EbMSResponseMessage> messageQueue,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory,
			@NonNull PlatformTransactionManager transactionManager,
			@NonNull JmsTemplate jmsTemplate)
	{
		super(messageQueue, cpaManager, ebMSClientFactory);
		this.transactionManager = transactionManager;
		this.jmsTemplate = jmsTemplate;
	}

	@Override
	public Optional<EbMSResponseMessage> sendMessage(final EbMSRequestMessage message) throws EbMSProcessorException
	{
		try
		{
			val messageHeader = message.getMessageHeader();
			val uri = getUri(messageHeader);
			log.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
			val response = createClient(messageHeader).sendMessage(uri, EbMSMessageUtils.getEbMSDocument(message));
			if (response != null)
				return Optional.of((EbMSResponseMessage)EbMSMessageUtils.getEbMSMessage(response));
			else if (message.getSyncReply() == null)
			{
				return receiveMessage(messageHeader);
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

	private Optional<EbMSResponseMessage> receiveMessage(final org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader messageHeader)
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			jmsTemplate.setReceiveTimeout(3 * Constants.MINUTE_IN_MILLIS);
			val result = jmsTemplate.receiveSelectedAndConvert(JMS_DESTINATION_NAME, "JMSCorrelationID='" + messageHeader.getMessageData().getMessageId() + "'");
			transactionManager.commit(status);
			return Optional.ofNullable((EbMSResponseMessage)result);
		}
		catch (JmsException e)
		{
			transactionManager.rollback(status);
			throw new EbMSProcessorException(e);
		}
	}

	@Override
	public void handleResponseMessage(final EbMSResponseMessage message) throws EbMSProcessorException
	{
		val status = transactionManager.getTransaction(null);
		try
		{
			jmsTemplate.setExplicitQosEnabled(true);
			jmsTemplate.setTimeToLive(Constants.MINUTE_IN_MILLIS);
			jmsTemplate.convertAndSend(JMS_DESTINATION_NAME, message, m ->
			{
				m.setJMSCorrelationID(message.getMessageHeader().getMessageData().getRefToMessageId());
				// m.setJMSExpiration(Constants.MINUTE_IN_MILLIS);
				return m;
			});
			transactionManager.commit(status);
		}
		catch (JmsException e)
		{
			transactionManager.commit(status);
			throw new EbMSProcessorException(e);
		}
	}

	@Async("deliveryManagerTaskExecutor")
	@Override
	public void sendResponseMessage(final String uri, final EbMSBaseMessage response) throws EbMSProcessorException
	{
		try
		{
			log.info("Sending message " + response.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
			createClient(response.getMessageHeader()).sendMessage(uri, EbMSMessageUtils.getEbMSDocument(response));
		}
		catch (SOAPException | JAXBException | ParserConfigurationException | SAXException | IOException | TransformerFactoryConfigurationError
				| TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
	}
}
