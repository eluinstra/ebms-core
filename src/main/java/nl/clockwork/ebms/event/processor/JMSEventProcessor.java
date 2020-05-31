package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;
import nl.clockwork.ebms.jms.JmsTemplateFactory;

@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class JMSEventProcessor implements Runnable
{
	JmsTemplate jmsTemplate;
	@NonNull
	HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype;

	@Builder(setterPrefix = "set")
	public JMSEventProcessor(
			@NonNull EventManagerType type,
			@NonNull String jmsBrokerUrl,
			int maxThreads,
			@NonNull HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype)
	{
		if (type == EventManagerType.JMS)
		{
//			val container = createMessageListenerContainer(jmsBrokerUrl,maxThreads);
//			container.initialize();
			jmsTemplate = createJmsTemplate(jmsBrokerUrl);
			IntStream.range(0,maxThreads).forEach(i -> startDeamon());
		}
		else
			this.jmsTemplate = null;
		this.handleEventTaskPrototype = handleEventTaskPrototype;
	}

	private void startDeamon()
	{
		val thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	private JmsTemplate createJmsTemplate(String jmsBrokerUrl)
	{
		val jmsTemplate = JmsTemplateFactory.getInstance(jmsBrokerUrl);
		jmsTemplate.setSessionTransacted(true);
		jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return jmsTemplate;
	}

	private DefaultMessageListenerContainer createMessageListenerContainer(String jmsBrokerUrl, int maxThreads)
	{
		val container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(new PooledConnectionFactory(jmsBrokerUrl));
		container.setDestinationName(JMSEventManager.JMS_DESTINATION_NAME);
		container.setMessageListener(new MessageListener()
		{
			@Override
			public void onMessage(Message message)
			{
				try
				{
					val event = createEvent(message);
					val task = handleEventTaskPrototype.setEvent(event).build();
					task.run();
					message.acknowledge();
				}
				catch (JMSException e)
				{
					throw new RuntimeException(e);
				}
			}

		});
		container.setSessionTransacted(true);
		container.setConcurrentConsumers(maxThreads);
		return container;
	}

  public void run()
  {
  	while (true)
  	{
			try
			{
				val message = jmsTemplate.receive(JMSEventManager.JMS_DESTINATION_NAME);
				val event = createEvent(message);
				val task = handleEventTaskPrototype.setEvent(event).build();
				task.run();
				message.acknowledge();
			}
			catch (Exception e)
			{
			}
  	}
  }

	private EbMSEvent createEvent(Message message) throws JMSException
	{
		val result = EbMSEvent.builder()
				.cpaId(message.getStringProperty("cpaId"))
				.sendDeliveryChannelId(message.getStringProperty("sendDeliveryChannelId"))
				.receiveDeliveryChannelId(message.getStringProperty("receiveDeliveryChannelId"))
				.messageId(message.getStringProperty("messageId"))
				.timeToLive(Instant.parse(message.getStringProperty("timeToLive")))
				.timestamp(Instant.parse(message.getStringProperty("timestamp")))
				.confidential(message.getBooleanProperty("confidential"))
				.retries(message.getIntProperty("retries"))
				.build();
		return result;
	}
}
