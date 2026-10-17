/*
 * Copyright 2011 - 2026 Clockwork
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
package nl.clockwork.ebms.migrate;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;
import lombok.val;

/**
 * Writes the migrated properties of an {@link MigrateResult} to a Java properties file (the format read by the 2.21 embedded server and the Spring
 * {@code @Value} beans).
 */
public final class ServerPropertiesWriter
{
	private final Map<String, String> properties;

	public ServerPropertiesWriter(Map<String, String> properties)
	{
		this.properties = properties;
	}

	public void write(Writer writer) throws IOException
	{
		val javaProperties = new Properties();
		properties.forEach(javaProperties::setProperty);
		javaProperties.store(writer, "EbMS Server properties (migrated from EbMS Admin 2.20.x command line parameters)");
		writer.flush();
	}
}
