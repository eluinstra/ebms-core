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
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import nl.clockwork.ebms.common.CPAManager;
import nl.clockwork.ebms.model.CacheablePartyId;
import nl.clockwork.ebms.model.EbMSDocument;
import nl.clockwork.ebms.model.EbMSMessage;
import nl.clockwork.ebms.processor.EbMSProcessingException;
import nl.clockwork.ebms.processor.EbMSProcessorException;
import nl.clockwork.ebms.util.CPAUtils;
import nl.clockwork.ebms.util.EbMSMessageUtils;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.xml.sax.SAXException;

public class DeliveryManagerMIC extends DeliveryManager
{
	private CPAManager cpaManager;
	private EbMSHttpMIClientFactory ebMSHttpMIClientFactory;

	public EbMSMessage sendMessage(final String uri, final EbMSMessage message) throws EbMSProcessorException
	{
		try
		{
			MessageHeader rmh = message.getMessageHeader();
			String clientAlias = cpaManager.getClientAlias(rmh.getCPAId(),new CacheablePartyId(rmh.getFrom().getPartyId()),rmh.getFrom().getRole(),CPAUtils.toString(rmh.getService()),rmh.getAction());
			if (message.getSyncReply() == null)
			{
				try
				{
					messageQueue.register(message.getMessageHeader().getMessageData().getMessageId());
					logger.info("Sending message " + message.getMessageHeader().getMessageData().getMessageId() + " to " + uri + " using clientAlias " + clientAlias);
					EbMSDocument document = ebMSHttpMIClientFactory.getEbMSClient(clientAlias).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
					if (document == null)
						return messageQueue.get(message.getMessageHeader().getMessageData().getMessageId());
					else
					{
						messageQueue.remove(message.getMessageHeader().getMessageData().getMessageId());
						return EbMSMessageUtils.getEbMSMessage(document);
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
				logger.info("Sending message " + message.getMessageHeader().getMessageData().getMessageId() + " to " + uri + " using clientAlias " + clientAlias);
				EbMSDocument response = ebMSHttpMIClientFactory.getEbMSClient(clientAlias).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(message));
				if (response != null)
					return EbMSMessageUtils.getEbMSMessage(response);
			}
			return null;
		}
		catch (SOAPException | JAXBException | SAXException | IOException | TransformerException e)
		{
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException | TransformerFactoryConfigurationError | XPathExpressionException | KeyStoreException| CertificateException e)
		{
			throw new EbMSProcessorException(e);
		}
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
					MessageHeader rmh = response.getMessageHeader();
					String clientAlias = cpaManager.getClientAlias(rmh.getCPAId(),new CacheablePartyId(rmh.getFrom().getPartyId()),rmh.getFrom().getRole(),CPAUtils.toString(rmh.getService()),rmh.getAction());
					logger.info("Sending message " + response.getMessageHeader().getMessageData().getMessageId() + " to " + uri + " using clientAlias " + clientAlias);
					ebMSHttpMIClientFactory.getEbMSClient(clientAlias).sendMessage(uri,EbMSMessageUtils.getEbMSDocument(response));
				}
				catch (Exception e)
				{
					logger.error("",e);
				}
			}
		};
		executorService.execute(command);
	}

	public void setCpaManager(CPAManager cpaManager)
	{
		this.cpaManager = cpaManager;
	}

	public void setEbMSHttpMIClientFactory(EbMSHttpMIClientFactory ebMSHttpMIClientFactory)
	{
		this.ebMSHttpMIClientFactory = ebMSHttpMIClientFactory;
	}
}
