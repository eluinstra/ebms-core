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
package nl.clockwork.ebms.event.listener;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSMessageEventType
{
	RECEIVED(0), DELIVERED(1), FAILED(2), EXPIRED(3);

	private static final List<Integer> IDS = Collections.unmodifiableList(stream().map(t -> t.id).collect(Collectors.toList()));
	int id;

	public static Stream<EbMSMessageEventType> stream()
	{
		return Stream.of(values());
	}

	public static Optional<EbMSMessageEventType> get(int id)
	{
		return stream().filter(s -> s.getId() == id).findFirst();
	}

	public static List<Integer> getIds()
	{
		return IDS;
	}

	public static List<Integer> getIds(EbMSMessageEventType...types)
	{
		return Stream.of(types).map(t -> t.id).collect(Collectors.toList());
	}
}