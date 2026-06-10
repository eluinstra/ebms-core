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
package nl.clockwork.ebms.common.event;

import com.google.common.base.Splitter;
import java.util.EnumSet;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.beans.factory.config.BeanPostProcessor;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class MessageEventListenerFilterProcessor implements BeanPostProcessor
{
	String filterCsv;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
	{
		if (!(bean instanceof MessageEventListener listener))
			return bean;
		if (bean instanceof MessageEventListenerFilter)
			return bean;
		val filter = Splitter.on(',')
				.trimResults()
				.omitEmptyStrings()
				.splitToStream(filterCsv == null ? "" : filterCsv)
				.map(MessageEventType::valueOf)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(MessageEventType.class)));
		return filter.isEmpty() ? bean : new MessageEventListenerFilter(filter, listener);
	}
}
