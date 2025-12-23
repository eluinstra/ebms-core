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
package nl.clockwork.ebms.common;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSMessageStatus
{
	UNAUTHORIZED(0),
	NOT_RECOGNIZED(1),
	RECEIVED(2),
	PROCESSED(3),
	FORWARDED(4),
	FAILED(5),
	/* WAITING(6,MessageStatusType.RECEIVED), */
	CREATED(10),
	DELIVERY_FAILED(11),
	DELIVERED(12),
	EXPIRED(13);

	private static final Set<EbMSMessageStatus> RECEIVE_STATUS = EnumSet.of(UNAUTHORIZED, NOT_RECOGNIZED, RECEIVED, PROCESSED, FORWARDED, FAILED);
	private static final Set<EbMSMessageStatus> SEND_STATUS = EnumSet.of(CREATED, DELIVERY_FAILED, DELIVERED, EXPIRED);
	int id;

	public static Stream<EbMSMessageStatus> stream()
	{
		return Stream.of(values());
	}

	public static Optional<EbMSMessageStatus> get(int id)
	{
		return stream().filter(s -> s.getId() == id).findFirst();
	}

	public static Optional<EbMSMessageStatus> get(String name)
	{
		return stream().filter(s -> s.name().equals(name)).findFirst();
	}

	public static EbMSMessageStatus[] getReceiveStatus()
	{
		return RECEIVE_STATUS.toArray(new EbMSMessageStatus[]{});
	}

	public static EbMSMessageStatus[] getSendStatus()
	{
		return SEND_STATUS.toArray(new EbMSMessageStatus[]{});
	}
}