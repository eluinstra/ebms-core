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
package nl.clockwork.ebms.plugin.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdkSerializationRoundTripTest
{
	@Test
	void serializesAndDeserializesTrustedClass()
	{
		final JdkSerializationSerializer ser = new JdkSerializationSerializer();
		final JdkSerializationDeserializer de = new JdkSerializationDeserializer();
		final byte[] bytes = ser.serialize("t", "hello"); // java.lang.String — in allow-list
		assertThat(de.deserialize("t", bytes)).isEqualTo("hello");
	}

	@Test
	void serializeNullReturnsNull()
	{
		assertThat(new JdkSerializationSerializer().serialize("t", null)).isNull();
		assertThat(new JdkSerializationDeserializer().deserialize("t", null)).isNull();
	}

	@Test
	void deserializeRejectsUntrustedClass()
	{
		// org.junit.jupiter.api.* is not in the allow-list (nl.clockwork.ebms.*, java.*, javax.*, org.oasis_open.*).
		// We need a Serializable from an untrusted package; mockito.MockSettings isn't serializable, so we hand-craft
		// a stream that names an untrusted Serializable class. java.io.File is trusted (java.*); use one outside that.
		// org.opentest4j.AssertionFailedError is Serializable and lives in org.opentest4j (untrusted).
		final org.opentest4j.AssertionFailedError untrusted = new org.opentest4j.AssertionFailedError("payload");
		final byte[] bytes = new JdkSerializationSerializer().serialize("t", untrusted);
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JdkSerializationDeserializer().deserialize("t", bytes))
				.isInstanceOf(org.apache.kafka.common.errors.SerializationException.class)
				.hasMessageContaining("not allowed");
	}
}
