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
package nl.clockwork.ebms.event;

import java.util.Map;

import javax.jms.Destination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.JmsTemplate;

public class JMSEventListener implements EventListener
{
	protected transient Log logger = LogFactory.getLog(getClass());
	private JmsTemplate jmsTemplate;
	private Map<String,Destination> destinations;

	@Override
	public void onMessageReceived(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " received");
		jmsTemplate.send(destinations.get("EVENT.RECEIVED"),new EventMessageCreator(messageId));
	}

	@Override
	public void onMessageAcknowledged(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " acknowledged");
		jmsTemplate.send(destinations.get("EVENT.ACKNOWLEDGED"),new EventMessageCreator(messageId));
	}
	
	@Override
	public void onMessageDeliveryFailed(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " delivery failed");
		jmsTemplate.send(destinations.get("EVENT.FAILED"),new EventMessageCreator(messageId));
	}

	@Override
	public void onMessageNotAcknowledged(String messageId) throws EventException
	{
		logger.info("Message " + messageId + " not acknowledged");
		jmsTemplate.send(destinations.get("EVENT.EXPIRED"),new EventMessageCreator(messageId));
	}

	public void setJmsTemplate(JmsTemplate jmsTemplate)
	{
		this.jmsTemplate = jmsTemplate;
	}
	
	public void setDestinations(Map<String,Destination> destinations)
	{
		this.destinations = destinations;
	}
}
