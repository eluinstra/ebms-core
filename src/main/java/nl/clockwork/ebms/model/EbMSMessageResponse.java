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

import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.w3._2000._09.xmldsig.SignatureType;

public abstract class EbMSMessageResponse extends EbMSResponseMessage
{
	private static final long serialVersionUID = 1L;

	public EbMSMessageResponse(MessageHeader messageHeader, SignatureType signature)
	{
		super(messageHeader,signature);
	}
}
