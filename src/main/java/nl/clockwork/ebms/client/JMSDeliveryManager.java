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

import nl.clockwork.ebms.Constants;
import nl.clockwork.ebms.EbMSMessageUtils;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.xml.sax.SAXException;

public class JMSDeliveryManager extends DeliveryManager //DeliveryService
{
	private static final String MESSAGE = "MESSAGE";
	private transient Log logger = LogFactory.getLog(getClass());
	private JmsTemplate jmsTemplate;

	@Override
	public Optional<EbMSMessage> sendMessage(final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			MessageHeader messageHeader = message.getMessageHeader();
			final String uri = getUri(messageHeader);
			if (message.getSyncReply() == null)
			{
				logger.info("Sending message " + messageHeader.getMessageData().getMessageId() + " to " + uri);
				EbMSDocument document = createClient(messageHeader).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
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
				logger.info("Sending message " + response.getMessageHeader().getMessageData().getMessageId() + " to " + uri);
				createClient(response.getMessageHeader()).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
			}
			catch (Exception e)
			{
				logger.error("",e);
			}
		};
		executorService.execute(command);
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate)
	{
		this.jmsTemplate = jmsTemplate;
	}

}
