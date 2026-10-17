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
package nl.clockwork.ebms.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.TreeMap;
import lombok.val;

/**
 * Reads and writes an ebms-server.properties file.
 */
public class ServerPropertiesFile
{
	public static final String EBMS_SERVER_PROPERTIES = "ebms-server.properties";

	private ServerPropertiesFile()
	{
		// utility
	}

	public static TreeMap<String, String> read(File file) throws IOException
	{
		val result = new TreeMap<String, String>();
		if (file.exists())
		{
			val properties = new Properties();
			try (val in = new FileInputStream(file))
			{
				properties.load(in);
			}
			for (val name : properties.stringPropertyNames())
			{
				result.put(name, properties.getProperty(name));
			}
		}
		return result;
	}

	public static void write(File file, TreeMap<String, String> properties) throws IOException
	{
		try (val writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))
		{
			val javaProperties = new Properties();
			properties.forEach(javaProperties::setProperty);
			javaProperties.store(writer, "EbMS Server properties migrated from EbMS Admin 2.20.x startup options");
			writer.flush();
		}
	}
}
