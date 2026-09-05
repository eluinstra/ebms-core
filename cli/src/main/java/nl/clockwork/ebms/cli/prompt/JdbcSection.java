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
package nl.clockwork.ebms.cli.prompt;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import nl.clockwork.ebms.cli.properties.JdbcDriver;
import nl.clockwork.ebms.cli.properties.JdbcProperties;
import nl.clockwork.ebms.cli.properties.JdbcUrl;
import org.jline.prompt.Prompter;
import org.jline.reader.UserInterruptException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JdbcSection
{
	public static void prompt(Prompter prompter, JdbcProperties properties) throws UserInterruptException, IOException
	{
		val selected = Prompts.list(prompter, "jdbcDriver", "JDBC driver", jdbcDriverOptions(), properties.getDriver().name());
		properties.setDriver(JdbcDriver.valueOf(selected));
		val jdbcUrl = properties.getJdbcUrl();
		jdbcUrl.setHost(Prompts.input(prompter, "jdbcHost", "Database host", jdbcUrl.getHost()));
		jdbcUrl.setPort(Prompts.number(prompter, "jdbcPort", "Database port", jdbcUrl.getPort() == null ? JdbcUrl.DEFAULT_JDBC_PORT : jdbcUrl.getPort(), 1, 65535));
		jdbcUrl.setDatabase(Prompts.input(prompter, "jdbcDatabase", "Database name", jdbcUrl.getDatabase()));
		System.out.println("JDBC URL: " + properties.getUrl());
		properties.setUsername(Prompts.input(prompter, "jdbcUsername", "Database username", properties.getUsername()));
		properties.setPassword(Prompts.password(prompter, "jdbcPassword", "Database password", properties.getPassword()));
	}

	private static Map<String, String> jdbcDriverOptions()
	{
		val result = new LinkedHashMap<String, String>();
		Arrays.asList(JdbcDriver.values()).forEach(d -> result.put(d.name(), d.name() + " (" + d.getDriverClassName() + ")"));
		return result;
	}
}
