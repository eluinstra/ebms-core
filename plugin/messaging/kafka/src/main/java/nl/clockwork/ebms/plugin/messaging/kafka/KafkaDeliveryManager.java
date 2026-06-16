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
package nl.clockwork.ebms.plugin.messaging.kafka;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.soap.SOAPException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import nl.clockwork.ebms.client.api.DeliveryManager;
import nl.clockwork.ebms.client.api.EbMSClient;
import nl.clockwork.ebms.client.transport.http.EbMSHttpClientFactory;
import nl.clockwork.ebms.common.cpa.CPAManager;
import nl.clockwork.ebms.common.message.EbMSMessageUtils;
import nl.clockwork.ebms.common.model.EbMSBaseMessage;
import nl.clockwork.ebms.common.model.EbMSRequestMessage;
import nl.clockwork.ebms.common.model.EbMSResponseMessage;
import nl.clockwork.ebms.server.processing.EbMSProcessingException;
import nl.clockwork.ebms.server.processing.EbMSProcessorException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.scheduling.annotation.Async;
import org.xml.sax.SAXException;

/**
 * Kafka-backed {@link DeliveryManager}. Sends outbound EbMS messages over HTTP and, when the receiver answers asynchronously, awaits the correlated reply
 * pushed back onto the shared reply topic ({@code kafka.topic.messageReplies}). Each plugin instance subscribes to that topic with its own consumer group and
 * filters replies by {@code refToMessageId}; replies destined for other instances are ignored.
 */
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KafkaDeliveryManager implements DeliveryManager, MessageListener<String, Object>
{
	@NonNull
	CPAManager cpaManager;
	@NonNull
	EbMSHttpClientFactory ebMSClientFactory;
	@NonNull
	KafkaTemplate<String, Object> kafkaTemplate;
	@NonNull
	String replyTopic;
	long replyTimeoutMs;
	ConcurrentHashMap<String, CompletableFuture<EbMSResponseMessage>> pendingReplies = new ConcurrentHashMap<>();

	@Builder(builderMethodName = "kafkaDeliveryManagerBuilder")
	public KafkaDeliveryManager(
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory,
			@NonNull KafkaTemplate<String, Object> kafkaTemplate,
			@NonNull String replyTopic,
			long replyTimeoutMs)
	{
		this.cpaManager = cpaManager;
		this.ebMSClientFactory = ebMSClientFactory;
		this.kafkaTemplate = kafkaTemplate;
		this.replyTopic = replyTopic;
		this.replyTimeoutMs = replyTimeoutMs;
	}

	@Override
	public Optional<EbMSResponseMessage> sendMessage(final EbMSRequestMessage message) throws EbMSProcessorException
	{
		final String messageId = message.getMessageHeader().getMessageData().getMessageId();
		final boolean async = message.getSyncReply() == null;
		CompletableFuture<EbMSResponseMessage> pending = null;
		try
		{
			val messageHeader = message.getMessageHeader();
			val uri = cpaManager.getReceivingUri(messageHeader);
			if (async)
				pending = registerPending(messageId);
			log.info("Sending message " + messageId + " to " + uri);
			val response = createClient(messageHeader).sendMessage(uri, EbMSMessageUtils.getEbMSDocument(message));
			if (response != null)
			{
				if (async)
					removePending(messageId);
				return Optional.of((EbMSResponseMessage)EbMSMessageUtils.getEbMSMessage(response));
			}
			if (async)
				return awaitReply(messageId, pending);
			return Optional.empty();
		}
		catch (SOAPException | JAXBException | SAXException | IOException | TransformerException e)
		{
			if (async)
				removePending(messageId);
			throw new EbMSProcessingException(e);
		}
		catch (ParserConfigurationException | TransformerFactoryConfigurationError | XPathExpressionException e)
		{
			if (async)
				removePending(messageId);
			throw new EbMSProcessorException(e);
		}
	}

	@Override
	public void handleResponseMessage(final EbMSResponseMessage message) throws EbMSProcessorException
	{
		final String refToMessageId = message.getMessageHeader().getMessageData().getRefToMessageId();
		if (refToMessageId == null)
			throw new EbMSProcessorException("Response message has no refToMessageId");
		try
		{
			kafkaTemplate.send(replyTopic, refToMessageId, message).get(replyTimeoutMs, TimeUnit.MILLISECONDS);
		}
		catch (KafkaException | InterruptedException | ExecutionException | TimeoutException e)
		{
			if (e instanceof InterruptedException)
				Thread.currentThread().interrupt();
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

	@Override
	public void onMessage(ConsumerRecord<String, Object> record)
	{
		final String refToMessageId = record.key();
		if (refToMessageId == null)
			return;
		final CompletableFuture<EbMSResponseMessage> future = pendingReplies.remove(refToMessageId);
		if (future == null)
			return;
		final Object value = record.value();
		if (value instanceof EbMSResponseMessage rsp)
			future.complete(rsp);
		else
		{
			log.warn("Reply on topic {} for {} has unexpected payload type {}", record.topic(), refToMessageId, value == null ? "null" : value.getClass().getName());
			future.complete(null);
		}
	}

	CompletableFuture<EbMSResponseMessage> registerPending(String messageId)
	{
		final CompletableFuture<EbMSResponseMessage> future = new CompletableFuture<>();
		pendingReplies.put(messageId, future);
		return future;
	}

	void removePending(String messageId)
	{
		pendingReplies.remove(messageId);
	}

	private Optional<EbMSResponseMessage> awaitReply(String messageId, CompletableFuture<EbMSResponseMessage> pending) throws EbMSProcessorException
	{
		try
		{
			return Optional.ofNullable(pending.get(replyTimeoutMs, TimeUnit.MILLISECONDS));
		}
		catch (TimeoutException e)
		{
			removePending(messageId);
			return Optional.empty();
		}
		catch (InterruptedException e)
		{
			removePending(messageId);
			Thread.currentThread().interrupt();
			throw new EbMSProcessorException(e);
		}
		catch (ExecutionException e)
		{
			removePending(messageId);
			throw new EbMSProcessorException(e.getCause() != null ? e.getCause() : e);
		}
	}

	private EbMSClient createClient(MessageHeader messageHeader)
	{
		val clientAlias = cpaManager.getSSLClientAlias(messageHeader).orElse(null);
		return ebMSClientFactory.getEbMSClient(clientAlias);
	}
}
