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
package nl.clockwork.ebms.cli.properties;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Optional;
import java.util.Scanner;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class JdbcUrl
{
	public static final int DEFAULT_JDBC_PORT = 9001;

	@NonNull
	String host = "localhost";
	Integer port = DEFAULT_JDBC_PORT;
	@NonNull
	String database = "ebms";

	/**
	 * Splits a JDBC URL like {@code jdbc:h2:tcp://localhost:9092/tmp/ebms} into host, port and database.
	 *
	 * @param jdbcURL the JDBC URL to parse
	 * @return the parsed host, port (or empty) and database, or empty if the URL cannot be parsed
	 */
	public static Optional<JdbcUrl> parse(String jdbcURL)
	{
		if (StringUtils.isEmpty(jdbcURL))
			return Optional.empty();
		try (Scanner scanner = new Scanner(jdbcURL))
		{
			String protocol = scanner.findInLine("(://|@|:@//)");
			if (protocol != null)
			{
				String urlString = scanner.findInLine("[^/:]+(:\\d+){0,1}");
				scanner.findInLine("(/|:|;databaseName=)");
				String database = scanner.findInLine("[^;]*");
				if (urlString != null)
				{
					java.net.URL url = URI.create("http://" + urlString).toURL();
					return Optional.of(JdbcUrl.of(url.getHost(), url.getPort() == -1 ? null : url.getPort(), database));
				}
			}
			return Optional.empty();
		}
		catch (MalformedURLException e)
		{
			return Optional.empty();
		}
	}
}
