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
package nl.clockwork.ebms.common.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.val;
import nl.clockwork.ebms.common.model.Party;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.oasis_open.committees.ebxml_msg.schema.msg_header_2_0.PartyId;

class EbMSKeyGeneratorTest
{

	EbMSKeyGenerator keyGenerator = new EbMSKeyGenerator();

	@ParameterizedTest
	@MethodSource("validInput")
	void testKeyGenerator(Method method, Object[] params, String expected)
	{
		assertEquals(expected, keyGenerator.generate(this, Objects.requireNonNull(method), Objects.requireNonNull(params)));
	}

	public static Stream<Arguments> validInput() throws NoSuchMethodException, SecurityException
	{
		val method = Object.class.getMethod("toString");
		return Stream.of(
				of(method, new Object[]{}, "toString[]"),
				of(method, new Object[]{null}, "toString[]"),
				of(method, new Object[]{"1"}, "toString[1]"),
				of(method, new Object[]{null, "1"}, "toString[,1]"),
				of(method, new Object[]{"1", null}, "toString[1,]"),
				of(method, new Object[]{"1", "2"}, "toString[1,2]"),
				of(method, new Object[]{"1", Party.of("1", "role")}, "toString[1,Party(partyId=1, role=role)]"),
				of(method, new Object[]{"1", "2", "3"}, "toString[1,2,3]"),
				of(method, new Object[]{"1", Party.of("1", "role"), "3"}, "toString[1,Party(partyId=1, role=role),3]"),
				of(method, new Object[]{List.of("1", "2", "3")}, "toString[[1, 2, 3]]"),
				of(method, new Object[]{"1", List.of(Party.of("1", "role")), "3"}, "toString[1,[Party(partyId=1, role=role)],3]"),
				of(
						method,
						new Object[]{"1", List.of(Party.of("1", "role"), Party.of("2", "role")), "3"},
						"toString[1,[Party(partyId=1, role=role), Party(partyId=2, role=role)],3]"),
				of(method, new Object[]{"1", List.of(createPartyId("1", "type")), "3"}, "toString[1,[type:1],3]"),
				of(method, new Object[]{"1", List.of(createPartyId("1", "type"), createPartyId("2", "type")), "3"}, "toString[1,[type:1, type:2],3]"));
	}

	private static PartyId createPartyId(String value, String type)
	{
		val partyId = new PartyId();
		partyId.setValue(value);
		partyId.setType(type);
		return partyId;
	}
}
