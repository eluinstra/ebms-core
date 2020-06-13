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
package nl.clockwork.ebms.event.processor;

import java.time.Instant;

import org.oasis_open.committees.ebxml_cppa.schema.cpp_cpa_2_0.DeliveryChannel;
import org.springframework.beans.factory.FactoryBean;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import lombok.experimental.FieldDefaults;
import nl.clockwork.ebms.cpa.CPAManager;
import nl.clockwork.ebms.cpa.CPAUtils;
import nl.clockwork.ebms.dao.EbMSDAO;
import nl.clockwork.ebms.jms.JmsTemplateFactory;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventManagerFactory implements FactoryBean<EventManager>
{
	public enum EventManagerType
	{
		DEFAULT, JMS;
	}

	EventManager eventManager;

	@Builder(setterPrefix = "set")
	public EventManagerFactory(
			@NonNull EventManagerType type,
			@NonNull EbMSDAO ebMSDAO,
			@NonNull EbMSEventDAO ebMSEventDAO,
			@NonNull CPAManager cpaManager,
			String serverId,
			@NonNull String jmsBrokerUrl,
			int nrAutoRetries,
			int autoRetryInterval)
	{
		switch(type)
		{
			case JMS:
				eventManager = new JMSEventManager(JmsTemplateFactory.getInstance(jmsBrokerUrl),ebMSDAO,ebMSEventDAO,cpaManager,nrAutoRetries,autoRetryInterval);
				break;
			default:
				eventManager = new EbMSEventManager(ebMSDAO,ebMSEventDAO,cpaManager,serverId,nrAutoRetries,autoRetryInterval);
		}
	}
	
	@Override
	public EventManager getObject() throws Exception
	{
		return eventManager;
	}
	
	@Override
	public Class<?> getObjectType()
	{
		return EventManager.class;
	}
	
	@Override
	public boolean isSingleton()
	{
		return true;
	}
	
	public static EbMSEvent createNextEvent(EbMSEvent event, DeliveryChannel deliveryChannel)
	{
		val rm = CPAUtils.getReceiverReliableMessaging(deliveryChannel);
		val timestamp = event.getRetries() < rm.getRetries().intValue() ? Instant.now().plus(rm.getRetryInterval()) : event.getTimeToLive();
		return event.createNextEvent(timestamp);
	}

	public static EbMSEvent createNextEvent(EbMSEvent event, long retryInterval)
	{
		val timestamp = Instant.now().plusSeconds(60 * retryInterval);
		return event.createNextEvent(timestamp);
	}
}
