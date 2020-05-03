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
package nl.clockwork.ebms.model;

import java.io.Serializable;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import nl.clockwork.ebms.EbMSMessageStatus;

@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class EbMSMessageContext implements Serializable
{
	private static final long serialVersionUID = 1L;
	@NonNull
	String cpaId;
	@NonNull
	Role fromRole;
	@XmlElement
	Role toRole;
	//@NonNull
	String service;
	//@NonNull
	String action;
	@XmlElement
	Date timestamp;
	@XmlElement
	String conversationId;
	@XmlElement
	String messageId;
	@XmlElement
	String refToMessageId;
	@XmlElement
	EbMSMessageStatus messageStatus;
}
