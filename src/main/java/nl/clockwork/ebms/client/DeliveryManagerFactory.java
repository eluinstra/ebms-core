package nl.clockwork.ebms.client;

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.jms.JMSUtils;
import nl.clockwork.ebms.model.EbMSResponseMessage;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryManagerFactory implements FactoryBean<DeliveryManager>
{
	public enum DeliveryManagerType
	{
		DEFAULT, JMS;
	}

	@Builder(setterPrefix = "set")
	public DeliveryManagerFactory(
			@NonNull DeliveryManagerType type,
			Integer maxThreads,
			Integer processorsScaleFactor,
			Integer queueScaleFactor,
			@NonNull MessageQueue<EbMSResponseMessage> messageQueue,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory,
			@NonNull String jmsBrokerURL)
	{
		switch(type)
		{
			case JMS:
				deliveryManager = JMSDeliveryManager.jmsDeliveryManagerBuilder()
						.maxThreads(maxThreads)
						.processorsScaleFactor(processorsScaleFactor)
						.queueScaleFactor(queueScaleFactor)
						.messageQueue(messageQueue)
						.cpaManager(cpaManager)
						.ebMSClientFactory(ebMSClientFactory)
						.jmsTemplate(JMSUtils.createJmsTemplate(jmsBrokerURL))
						.build();
				break;
			default:
				deliveryManager = DeliveryManager.builder()
						.maxThreads(maxThreads)
						.processorsScaleFactor(processorsScaleFactor)
						.queueScaleFactor(queueScaleFactor)
						.messageQueue(messageQueue)
						.cpaManager(cpaManager)
						.ebMSClientFactory(ebMSClientFactory)
						.build();
		}
	}

	@NonNull
	DeliveryManager deliveryManager;

	@Override
	public DeliveryManager getObject() throws Exception
	{
		return deliveryManager;
	}

	@Override
	public Class<?> getObjectType()
	{
		return DeliveryManager.class;
	}

}
