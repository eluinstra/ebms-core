package nl.clockwork.ebms.event.processor;

import java.time.Instant;
import java.util.stream.IntStream;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.core.JmsTemplate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import nl.clockwork.ebms.event.processor.EventManagerFactory.EventManagerType;
import nl.clockwork.ebms.jms.JmsTemplateFactory;

@Slf4j
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public class JMSEventProcessor implements Runnable
{
	JmsTemplate jmsTemplate;
	@NonNull
	HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype;

	@Builder(setterPrefix = "set")
	public JMSEventProcessor(
			boolean startEbMSClient,
			@NonNull EventManagerType type,
			@NonNull String jmsBrokerUrl,
			int maxThreads,
			@NonNull HandleEventTask.HandleEventTaskBuilder handleEventTaskPrototype)
	{
		if (startEbMSClient && type == EventManagerType.JMS)
		{
			jmsTemplate = createJmsTemplate(jmsBrokerUrl);
			IntStream.range(0,maxThreads).forEach(i -> startDeamon());
		}
		else
			this.jmsTemplate = null;
		this.handleEventTaskPrototype = handleEventTaskPrototype;
	}

	private JmsTemplate createJmsTemplate(String jmsBrokerUrl)
	{
		val jmsTemplate = JmsTemplateFactory.getInstance(jmsBrokerUrl);
		jmsTemplate.setSessionTransacted(true);
		jmsTemplate.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE);
		return jmsTemplate;
	}

	private void startDeamon()
	{
		val thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
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
				log.trace("",e);
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
