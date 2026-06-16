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
package nl.clockwork.ebms.server.embedded.config;

import java.io.IOException;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.springframework.core.io.Resource;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public class EbMSPropertySourcesPlaceholderConfigurer extends org.springframework.context.support.PropertySourcesPlaceholderConfigurer
{
	Resource overridePropertiesFile;

	@Override
	public void setLocations(@org.jspecify.annotations.NonNull Resource...locations)
	{
		overridePropertiesFile = locations[locations.length - 1];
		super.setLocations(locations);
	}

	public Resource getOverridePropertiesFile()
	{
		return overridePropertiesFile;
	}

	public Properties getProperties() throws IOException
	{
		val properties = mergeProperties();
		val result = new Properties();
		for (val key : properties.stringPropertyNames())
		{
			val systemProperty = System.getProperty(key);
			val envProperty = System.getenv(key);
			val envPropertyUnderscore = System.getenv(key.replace('.', '_'));
			val value = properties.getProperty(key);
			if (envPropertyUnderscore != null)
				result.setProperty(key, envPropertyUnderscore);
			else if (envProperty != null)
				result.setProperty(key, envProperty);
			else if (systemProperty != null)
				result.setProperty(key, systemProperty);
			else
				result.setProperty(key, value);
		}
		return result;
	}
}
