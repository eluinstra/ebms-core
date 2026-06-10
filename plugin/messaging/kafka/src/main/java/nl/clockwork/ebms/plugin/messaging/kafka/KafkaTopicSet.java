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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Bean exposing a set of {@link NewTopic} definitions. Spring Kafka's {@link org.springframework.kafka.core.KafkaAdmin} auto-discovers {@link NewTopic} beans
 * and {@link Iterable}&lt;{@link NewTopic}&gt; beans in the context and creates the topics on startup. This class lets us register multiple event topics behind
 * a single bean name.
 *
 * @see TopicBuilder
 */
public class KafkaTopicSet implements Iterable<NewTopic>
{
	private final List<NewTopic> topics = new ArrayList<>();

	public void add(NewTopic topic)
	{
		topics.add(topic);
	}

	public void addAll(Collection<NewTopic> ts)
	{
		topics.addAll(ts);
	}

	@Override
	public Iterator<NewTopic> iterator()
	{
		return topics.iterator();
	}
}
