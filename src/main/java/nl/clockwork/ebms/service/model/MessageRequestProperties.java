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
package nl.clockwork.ebms.service.model;


import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
// @RequiredArgsConstructor
@AllArgsConstructor
public class MessageRequestProperties implements Serializable
{
	private static final long serialVersionUID = 1L;
	@XmlElement(required = true)
	@NonNull
	String cpaId;
	@XmlElement(required = true)
	// @NonNull
	String fromPartyId;
	@XmlElement(required = true)
	// @NonNull
	String fromRole;
	String toPartyId;
	String toRole;
	@XmlElement(required = true)
	// @NonNull
	String service;
	@XmlElement(required = true)
	// @NonNull
	String action;
	String conversationId;
	String messageId;
	String refToMessageId;

	public MessageRequestProperties(@NonNull String cpaId, @NonNull Party fromParty, @NonNull String service, @NonNull String action)
	{
		this.cpaId = cpaId;
		this.fromPartyId = fromParty.getPartyId();
		this.fromRole = fromParty.getRole();
		this.service = service;
		this.action = action;
	}
}
