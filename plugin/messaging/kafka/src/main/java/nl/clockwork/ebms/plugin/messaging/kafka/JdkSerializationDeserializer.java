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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * JDK serialization-based Kafka {@link Deserializer}. Equivalent to the {@code JdkSerializationDeserializer} that Spring Kafka 3.x shipped and 4.x dropped.
 * Restricts deserialization to a single trusted class hierarchy to limit gadget-chain exposure inherent to Java serialization.
 */
public class JdkSerializationDeserializer implements Deserializer<Object>
{
	private static final String TRUSTED_PREFIX = "nl.clockwork.ebms.";

	@Override
	public Object deserialize(String topic, byte[] data)
	{
		if (data == null)
			return null;
		try (ObjectInputStream in = new TrustedObjectInputStream(new ByteArrayInputStream(data)))
		{
			return in.readObject();
		}
		catch (IOException | ClassNotFoundException e)
		{
			throw new SerializationException("Failed to deserialize value", e);
		}
	}

	private static final class TrustedObjectInputStream extends ObjectInputStream
	{
		TrustedObjectInputStream(ByteArrayInputStream in) throws IOException
		{
			super(in);
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException
		{
			final String name = desc.getName();
			if (name.startsWith(
					TRUSTED_PREFIX) || name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("[") || name.startsWith("org.oasis_open."))
				return super.resolveClass(desc);
			throw new SerializationException("Class is not allowed for Kafka JDK deserialization: " + name);
		}
	}
}
