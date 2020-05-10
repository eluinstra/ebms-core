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
package nl.clockwork.ebms;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageStatusType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSMessageStatus
{
	UNAUTHORIZED(0,MessageStatusType.UN_AUTHORIZED),
	NOT_RECOGNIZED(1,MessageStatusType.NOT_RECOGNIZED),
	RECEIVED(2,MessageStatusType.RECEIVED),
	PROCESSED(3,MessageStatusType.PROCESSED),
	FORWARDED(4,MessageStatusType.FORWARDED),
	FAILED(5,MessageStatusType.RECEIVED),
	/*WAITING(6,MessageStatusType.RECEIVED),*/
	SENDING(10),
	DELIVERY_FAILED(11),
	DELIVERED(12),
	EXPIRED(13);

	private static final Set<EbMSMessageStatus> RECEIVE_STATUS = EnumSet.of(UNAUTHORIZED,NOT_RECOGNIZED,RECEIVED,PROCESSED,FORWARDED,FAILED);
	private static final Set<EbMSMessageStatus> SEND_STATUS = EnumSet.of(SENDING,DELIVERY_FAILED,DELIVERED,EXPIRED);
	int id;
	MessageStatusType statusCode;

	private EbMSMessageStatus(int id)
	{
		this(id,null);
	}
	
	public static Stream<EbMSMessageStatus> stream()
	{
		return Stream.of(EbMSMessageStatus.values());
	}

	public static final EbMSMessageStatus get(int id)
	{
		return EbMSMessageStatus.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
		//orElseThrow(() -> new IllegalStateException("Unsupported EbMSMessageStatus Id: " + id));
	}

	public static final EbMSMessageStatus get(String name)
	{
		return EbMSMessageStatus.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
	}

	public static final EbMSMessageStatus get(MessageStatusType statusCode)
	{
		return EbMSMessageStatus.stream().filter(s -> s.statusCode.equals(statusCode)).findFirst().orElse(null);
	}

	public static final EbMSMessageStatus[] getReceiveStatus()
	{
		return RECEIVE_STATUS.toArray(new EbMSMessageStatus[]{});
	}

	public static final EbMSMessageStatus[] getSendStatus()
	{
		return SEND_STATUS.toArray(new EbMSMessageStatus[] {});
	}
}