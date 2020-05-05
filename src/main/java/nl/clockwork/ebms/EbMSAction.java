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

import java.util.stream.Stream;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
@Getter
public enum EbMSAction
{
	MESSAGE_ERROR("MessageError"),
	ACKNOWLEDGMENT("Acknowledgment"),
	STATUS_REQUEST("StatusRequest"),
	STATUS_RESPONSE("StatusResponse"),
	PING("Ping"),
	PONG("Pong");

	String action;

	public static Stream<EbMSAction> stream()
	{
		return Stream.of(EbMSAction.values());
	}
	
	public static final EbMSAction get(String action)
	{
		return EbMSAction.stream().filter(a -> a.action.equals(action)).findFirst().orElse(null);
	}

}