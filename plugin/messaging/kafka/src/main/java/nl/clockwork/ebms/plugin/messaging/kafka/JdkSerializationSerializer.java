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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

/**
 * JDK serialization-based Kafka {@link Serializer}. Equivalent to the {@code JdkSerializationSerializer} that Spring Kafka 3.x shipped and 4.x dropped.
 */
public class JdkSerializationSerializer implements Serializer<Object>
{
	@Override
	public byte[] serialize(String topic, Object data)
	{
		if (data == null)
			return null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(baos))
		{
			out.writeObject(data);
			out.flush();
			return baos.toByteArray();
		}
		catch (IOException e)
		{
			throw new SerializationException("Failed to serialize value of type " + data.getClass().getName(), e);
		}
	}
}
