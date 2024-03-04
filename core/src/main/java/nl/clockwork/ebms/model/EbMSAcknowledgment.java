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
package nl.clockwork.ebms.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Acknowledgment;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.SignatureType;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class EbMSAcknowledgment extends EbMSMessageResponse
{
	private static final long serialVersionUID = 1L;
	@NonNull
	Acknowledgment acknowledgment;

	@Builder
	public EbMSAcknowledgment(MessageHeader messageHeader, SignatureType signature, @NonNull Acknowledgment acknowledgment)
	{
		super(messageHeader, signature);
		this.acknowledgment = acknowledgment;
	}
}
