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

import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.EbMSThreadPoolExecutor;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.jms.JmsTemplateFactory;
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
			@NonNull EbMSThreadPoolExecutor ebMSThreadPoolExecutor,
			@NonNull MessageQueue<EbMSResponseMessage> messageQueue,
			@NonNull CPAManager cpaManager,
			@NonNull EbMSHttpClientFactory ebMSClientFactory,
			@NonNull String jmsBrokerURL)
	{
		switch(type)
		{
			case JMS:
				deliveryManager = JMSDeliveryManager.jmsDeliveryManagerBuilder()
						.ebMSThreadPoolExecutor(ebMSThreadPoolExecutor)
						.messageQueue(messageQueue)
						.cpaManager(cpaManager)
						.ebMSClientFactory(ebMSClientFactory)
						.jmsTemplate(JmsTemplateFactory.getInstance(jmsBrokerURL))
						.build();
				break;
			default:
				deliveryManager = DeliveryManager.builder()
						.ebMSThreadPoolExecutor(ebMSThreadPoolExecutor)
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

	@Override
	public boolean isSingleton()
	{
		return true;
	}
}
