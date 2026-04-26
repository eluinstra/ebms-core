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
package nl.clockwork.ebms.cache;

import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.From;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.MessageHeader;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.Service;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.To;

class MessageHeaderKeyGeneratorTest
{
	MessageHeaderKeyGenerator keyGenerator = new MessageHeaderKeyGenerator();

	@Test
	void testMessageHeaderKeyGenerator() throws NoSuchMethodException, SecurityException
	{
		val method = Object.class.getMethod("toString");
		val messageHeader = new MessageHeader();
		messageHeader.setCPAId("cpaId");
		messageHeader.setFrom(createFromPartyId("type", "fromPartyId"));
		messageHeader.setTo(createToPartyId("type", "toPartyId"));
		messageHeader.setService(createService("type", "service"));
		messageHeader.setAction("action");
		val key = keyGenerator.generate(null, method, messageHeader);
		Assertions.assertThat(key).isEqualTo("toString[cpaId,[type:toPartyId],[type:fromPartyId],type:service,action]");
	}

	private From createFromPartyId(String type, String value)
	{
		val result = new From();
		val fromPartyId = new PartyId();
		fromPartyId.setType(type);
		fromPartyId.setValue(value);
		result.getPartyId().add(fromPartyId);
		return result;
	}

	private To createToPartyId(String type, String value)
	{
		val result = new To();
		val toPartyId = new PartyId();
		toPartyId.setType(type);
		toPartyId.setValue(value);
		result.getPartyId().add(toPartyId);
		return result;
	}

	private Service createService(String type, String value)
	{
		Service result = new Service();
		result.setValue(value);
		result.setType(type);
		return result;
	}
}
