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
package nl.clockwork.ebms.server.embedded.startup;

import lombok.val;

public interface SystemInterface
{
	default void setProperty(String key, String value)
	{
		System.setProperty(key, value);
	}

	default String getProperty(String key)
	{
		return System.getProperty(key);
	}

	default String getProperty(String key, String defaultValue)
	{
		return System.getProperty(key, defaultValue);
	}

	default int getIntegerProperty(String key, int defaultValue)
	{
		val value = System.getProperty(key);
		return value != null ? Integer.parseInt(value) : defaultValue;
	}

	default boolean getBooleanProperty(String key, boolean defaultValue)
	{
		val value = System.getProperty(key);
		return value != null ? Boolean.parseBoolean(value) : defaultValue;
	}

	default void println(String s)
	{
		System.out.println(s);
	}

	default void printWarn(String s)
	{
		System.err.println(s);
	}

	default void exit(int status)
	{
		System.exit(status);
	}
}
