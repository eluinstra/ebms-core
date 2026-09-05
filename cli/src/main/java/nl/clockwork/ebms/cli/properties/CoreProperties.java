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
package nl.clockwork.ebms.cli.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig.EventListenerType;

@Data
@NoArgsConstructor
public class CoreProperties
{
	@NonNull
	EventListenerType eventListener = EventListenerType.DEFAULT;
	String jmsBrokerUrl;
	boolean jmsVirtualTopics;
	boolean startEmbeddedBroker;
	String activeMQConfigFile;
	boolean deleteMessageContentOnProcessed;

	public boolean isJms()
	{
		return EventListenerType.SIMPLE_JMS.equals(eventListener)
				|| EventListenerType.JMS.equals(eventListener)
				|| EventListenerType.JMS_TEXT.equals(eventListener);
	}
}
