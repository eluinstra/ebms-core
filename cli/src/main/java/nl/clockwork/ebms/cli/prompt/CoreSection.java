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
package nl.clockwork.ebms.cli.prompt;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.cli.properties.CoreProperties;
import nl.clockwork.ebms.common.event.MessageEventListenerConfig.EventListenerType;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreSection
{
	public static void prompt(Prompter prompter, CoreProperties properties) throws UserInterruptException, IOException
	{
		val selected = Prompts.list(prompter, "eventListener", "Message event listener", eventListenerOptions(), properties.getEventListener().name());
		properties.setEventListener(EventListenerType.valueOf(selected));
		if (properties.isJms())
			promptJms(prompter, properties);
		properties.setDeleteMessageContentOnProcessed(
				Prompts.confirm(prompter, "deleteMessageContentOnProcessed", "Delete message content on processed?", properties.isDeleteMessageContentOnProcessed()));
	}

	private static void promptJms(Prompter prompter, CoreProperties properties) throws UserInterruptException, IOException
	{
		properties.setJmsBrokerUrl(Prompts.input(prompter, "jmsBrokerUrl", "JMS broker URL", properties.getJmsBrokerUrl()));
		properties.setJmsVirtualTopics(Prompts.confirm(prompter, "jmsVirtualTopics", "Use JMS virtual topics?", properties.isJmsVirtualTopics()));
		properties.setStartEmbeddedBroker(Prompts.confirm(prompter, "startEmbeddedBroker", "Start embedded ActiveMQ broker?", properties.isStartEmbeddedBroker()));
		if (properties.isStartEmbeddedBroker())
			properties.setActiveMQConfigFile(
					Prompts.input(
							prompter,
							"activeMQConfigFile",
							"ActiveMQ config file (e.g. classpath:nl/clockwork/ebms/activemq.xml)",
							properties.getActiveMQConfigFile()));
	}

	private static Map<String, String> eventListenerOptions()
	{
		val result = new LinkedHashMap<String, String>();
		Arrays.asList(EventListenerType.values()).forEach(t -> result.put(t.name(), t.name()));
		return result;
	}
}
